package tests.debug.target;

import debug.module.JitInfo;
import debug.target.StackWalker;

import haxe.Int64;

class StackWalkerTest {
	public static function run(assert:Assert):Void {
		walksFramePointerChain(assert);
		stopsWhenNoFramePointer(assert);
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
