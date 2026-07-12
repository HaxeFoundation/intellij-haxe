package tests.debug.module;

import debug.DebugError;
import debug.module.JitInfoReader;

import haxe.Int64;
import haxe.io.BytesInput;
import haxe.io.BytesOutput;

class JitInfoReaderTest {
	public static function run(assert:Assert):Void {
		parses64BitHandshake(assert);
		parses32BitHandshake(assert);
		resolvesLargeOffsetFunctions(assert);
		rejectsBadMagic(assert);
		rejectsUnsupportedVersion(assert);
	}

	/** Builds a synthetic HLD1 handshake. Functions: Array of {start, offsets:[...]} (large inferred from values). */
	static function build(is64:Bool, flagsExtra:Int, hlVersionRaw:Int, pid:Int, jitCodeBase:Int, codeSize:Int,
			functions:Array<{start:Int, offsets:Array<Int>, ?large:Bool}>):BytesInput {
		var out = new BytesOutput();
		out.bigEndian = false;
		out.writeByte("H".code);
		out.writeByte("L".code);
		out.writeByte("D".code);
		out.writeByte("1".code);
		out.writeInt32((is64 ? 1 : 0) | flagsExtra);
		out.writeInt32(hlVersionRaw);
		out.writeInt32(pid);
		writePtr(out, is64, jitCodeBase - 0x10); // threads (arbitrary)
		writePtr(out, is64, jitCodeBase - 0x08); // globals (arbitrary)
		writePtr(out, is64, jitCodeBase);        // jitCode
		out.writeInt32(codeSize);
		writePtr(out, is64, jitCodeBase + 0x1000); // types (arbitrary)
		for (_ in 0...8) {
			out.writeInt32(4);
		}
		out.writeInt32(functions.length);
		for (fn in functions) {
			var nops = fn.offsets.length - 1;
			var large = fn.large != null ? fn.large : false;
			out.writeInt32(nops);
			out.writeInt32(fn.start);
			out.writeByte(large ? 1 : 0);
			for (o in fn.offsets) {
				if (large) {
					out.writeInt32(o);
				} else {
					out.writeUInt16(o);
				}
			}
		}
		return new BytesInput(out.getBytes());
	}

	static function writePtr(out:BytesOutput, is64:Bool, value:Int):Void {
		out.writeInt32(value);
		if (is64) {
			out.writeInt32(0);
		}
	}

	static function parses64BitHandshake(assert:Assert):Void {
		var base = 0x100000;
		var input = build(true, 4 | 8, 0x10f00, 22040, base, 200, [
			{start: 0, offsets: [0, 4, 8]},
			{start: 100, offsets: [0, 10]}
		]);
		var jit = JitInfoReader.read(input);

		assert.isTrue(jit.is64, "is64 flag");
		assert.isTrue(jit.threads, "threads flag");
		assert.isTrue(jit.winCall, "winCall flag");
		assert.equals(1, jit.hlVersionMajor, "hl major");
		assert.equals(15, jit.hlVersionMinor, "hl minor");
		assert.equals(22040, jit.pid, "pid");
		assert.equals(200, jit.codeSize, "codeSize");
		assert.equals(2, jit.functions.length, "function count");

		assert.isTrue(Int64.eq(Int64.add(Int64.ofInt(base), Int64.ofInt(4)), jit.addressOf(0, 1)), "addressOf(0,1)");
		var resolved = jit.resolveAddress(Int64.add(Int64.ofInt(base), Int64.ofInt(4)));
		assert.isTrue(resolved != null && resolved.fidx == 0 && resolved.op == 1, "resolveAddress base+4 -> fn0 op1");

		var inFn1 = jit.resolveAddress(Int64.add(Int64.ofInt(base), Int64.ofInt(105)));
		assert.isTrue(inFn1 != null && inFn1.fidx == 1 && inFn1.op == 0, "resolveAddress base+105 -> fn1 op0");

		assert.isTrue(jit.isCodePtr(Int64.add(Int64.ofInt(base), Int64.ofInt(199))), "isCodePtr within range");
		assert.isFalse(jit.isCodePtr(Int64.add(Int64.ofInt(base), Int64.ofInt(200))), "isCodePtr at end excluded");
		assert.isFalse(jit.isCodePtr(Int64.ofInt(base - 1)), "isCodePtr below base excluded");
		assert.isTrue(jit.resolveAddress(Int64.add(Int64.ofInt(base), Int64.ofInt(50))) == null, "gap address unresolved");
	}

	static function parses32BitHandshake(assert:Assert):Void {
		var base = 0x4000;
		var input = build(false, 0, 0x10c00, 999, base, 64, [
			{start: 0, offsets: [0, 8]}
		]);
		var jit = JitInfoReader.read(input);
		assert.isFalse(jit.is64, "32-bit is64 false");
		assert.equals(999, jit.pid, "32-bit pid");
		assert.isTrue(Int64.eq(Int64.ofInt(base), jit.addressOf(0, 0)), "32-bit addressOf(0,0)");
	}

	static function resolvesLargeOffsetFunctions(assert:Assert):Void {
		var base = 0x200000;
		// a "large" function whose offsets exceed uint16 range
		var input = build(true, 0, 0x10f00, 1, base, 200000, [
			{start: 0, offsets: [0, 70000, 150000], large: true}
		]);
		var jit = JitInfoReader.read(input);
		assert.isTrue(jit.functions[0].large, "large flag preserved");
		assert.equals(70000, jit.functions[0].offsets[1], "large offset value");
		var r = jit.resolveAddress(Int64.add(Int64.ofInt(base), Int64.ofInt(70000)));
		assert.isTrue(r != null && r.fidx == 0 && r.op == 1, "resolve large-offset op");
	}

	static function rejectsBadMagic(assert:Assert):Void {
		var out = new BytesOutput();
		out.writeByte("X".code);
		out.writeByte("X".code);
		out.writeByte("X".code);
		out.writeByte("1".code);
		try {
			JitInfoReader.read(new BytesInput(out.getBytes()));
			assert.fail("bad magic should throw");
		} catch (e:DebugError) {
			assert.isTrue(true, "bad magic throws DebugError");
		}
	}

	static function rejectsUnsupportedVersion(assert:Assert):Void {
		var out = new BytesOutput();
		out.writeByte("H".code);
		out.writeByte("L".code);
		out.writeByte("D".code);
		out.writeByte("9".code);
		try {
			JitInfoReader.read(new BytesInput(out.getBytes()));
			assert.fail("unsupported version should throw");
		} catch (e:DebugError) {
			// The message must say WHICH version kind mismatched (the handshake
			// protocol digit), what we saw, and what is supported.
			assert.isTrue(StringTools.contains(e.message, "handshake protocol version"),
				"names the version kind (was: " + e.message + ")");
			assert.isTrue(StringTools.contains(e.message, "HLD9"), "reports the received version");
			assert.isTrue(StringTools.contains(e.message, "HLD1"), "reports the supported version");
		}
	}
}
