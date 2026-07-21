package tests.debug.target;

import debug.module.JitInfo;
import debug.target.StackWalker;

import haxe.Int64;

class StackWalkerTest {
	public static function run(assert:Assert):Void {
		walksFramePointerChain(assert);
		stopsWhenNoFramePointer(assert);
		seedsFromCEntryViaCCaller(assert);
		seedsFromCEntryViaJitCaller(assert);
	}

	static function syntheticJit():JitInfo {
		return new JitInfo({
			is64: true, boolSize4: false, threads: true, winCall: true,
			hlVersionMajor: 1, hlVersionMinor: 15, hlVersionPatch: 0,
			pid: 1, threadsPtr: Int64.ofInt(0), globalsPtr: Int64.ofInt(0),
			jitCodeBase: Int64.ofInt(0x10000), codeSize: 0x1000, typesPtr: Int64.ofInt(0),
			structSizes: [0, 1, 2, 4, 8, 4, 8, 1, 8],
			functions: [
				{nops: 3, start: 0, large: false, offsets: [0, 4, 8, 12]},
				{nops: 2, start: 100, large: false, offsets: [0, 4, 8]}
			]
		});
	}

	static function pokePtr(api:FakeDebugApi, addr:Int, value:Int):Void {
		// low 32 bits only (test values fit in an Int); high dword is zero
		for (i in 0...4) {
			api.poke(Int64.ofInt(addr + i), (value >>> (8 * i)) & 0xFF);
		}
		for (i in 4...8) {
			api.poke(Int64.ofInt(addr + i), 0);
		}
	}

	static function walksFramePointerChain(assert:Assert):Void {
		var api = new FakeDebugApi();
		var jit = syntheticJit();
		var tid = 7;

		// innermost frame in fn0 at op1, caller frame base at 0x20000
		api.setRegister(tid, Eip, Int64.ofInt(0x10004));
		api.setRegister(tid, Ebp, Int64.ofInt(0x20000));

		// frame 0x20000: saved ebp -> 0x20010, return addr -> inside fn1 (op1)
		pokePtr(api, 0x20000, 0x20010);
		pokePtr(api, 0x20008, 0x10068);
		// frame 0x20010: chain terminates (ebp 0, non-code return)
		pokePtr(api, 0x20010, 0);
		pokePtr(api, 0x20018, 0);

		var frames = new StackWalker(api, 1, jit).walk(tid);
		assert.equals(2, frames.length, "two frames walked");
		assert.equals(0, frames[0].fidx, "top frame fn0");
		assert.equals(1, frames[0].op, "top frame op1");
		assert.isTrue(Int64.eq(frames[0].address, Int64.ofInt(0x10004)), "top frame address is EIP");
		assert.equals(1, frames[1].fidx, "caller frame fn1");
		assert.equals(1, frames[1].op, "caller frame op1");
	}

	// Stopped at hl_throw's entry (EIP in C, not JIT) reached via a C runtime
	// function (hl_null_access): the walk must unwind the C frame and report the
	// throwing Haxe frame as the top, using that frame's own base.
	static function seedsFromCEntryViaCCaller(assert:Assert):Void {
		var api = new FakeDebugApi();
		var jit = syntheticJit();
		var tid = 5;

		// EIP inside C code (hl_throw entry), RBP still the C caller's base
		api.setRegister(tid, Eip, Int64.ofInt(0x99000));
		api.setRegister(tid, Esp, Int64.ofInt(0x30000));
		api.setRegister(tid, Ebp, Int64.ofInt(0x30010));

		// [Esp] = return into hl_null_access (C, not JIT)
		pokePtr(api, 0x30000, 0x99500);
		// hl_null_access frame @0x30010: saved ebp -> 0x30020 (Haxe frame base),
		// return addr -> inside fn1 op1 (JIT)
		pokePtr(api, 0x30010, 0x30020);
		pokePtr(api, 0x30018, 0x10068);
		// Haxe frame @0x30020: chain terminates
		pokePtr(api, 0x30020, 0);
		pokePtr(api, 0x30028, 0);

		var frames = new StackWalker(api, 1, jit).walk(tid);
		assert.equals(1, frames.length, "one Haxe frame recovered from the C entry");
		assert.equals(1, frames[0].fidx, "throwing frame fn1");
		assert.equals(1, frames[0].op, "throwing frame op1");
		assert.isTrue(Int64.eq(frames[0].ebp, Int64.ofInt(0x30020)), "throwing frame uses its own base");
	}

	// Stopped at hl_throw's entry reached DIRECTLY from jitted code (a bytecode
	// OThrow): [Esp] is already a JIT return address, so the throwing frame's base
	// is the current RBP.
	static function seedsFromCEntryViaJitCaller(assert:Assert):Void {
		var api = new FakeDebugApi();
		var jit = syntheticJit();
		var tid = 6;

		api.setRegister(tid, Eip, Int64.ofInt(0x99000)); // hl_throw entry (C)
		api.setRegister(tid, Esp, Int64.ofInt(0x30000));
		api.setRegister(tid, Ebp, Int64.ofInt(0x30020)); // the throwing frame's base

		pokePtr(api, 0x30000, 0x10068); // [Esp] = return into fn1 op1 (JIT)
		pokePtr(api, 0x30020, 0); // frame chain terminates
		pokePtr(api, 0x30028, 0);

		var frames = new StackWalker(api, 1, jit).walk(tid);
		assert.equals(1, frames.length, "one Haxe frame recovered from the direct JIT caller");
		assert.equals(1, frames[0].fidx, "throwing frame fn1");
		assert.equals(1, frames[0].op, "throwing frame op1");
		assert.isTrue(Int64.eq(frames[0].ebp, Int64.ofInt(0x30020)), "throwing frame base is the current RBP");
	}

	static function stopsWhenNoFramePointer(assert:Assert):Void {
		var api = new FakeDebugApi();
		var jit = syntheticJit();
		var tid = 3;
		api.setRegister(tid, Eip, Int64.ofInt(0x10008));
		api.setRegister(tid, Ebp, Int64.ofInt(0)); // no frame pointer

		var frames = new StackWalker(api, 1, jit).walk(tid);
		assert.equals(1, frames.length, "only the top frame without a frame pointer");
		assert.equals(0, frames[0].fidx, "top frame fn0");
		assert.equals(2, frames[0].op, "top frame op2");
	}
}
