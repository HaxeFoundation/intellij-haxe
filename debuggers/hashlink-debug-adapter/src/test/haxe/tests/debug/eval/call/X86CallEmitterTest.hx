package tests.debug.eval.call;

import debug.eval.call.X86CallEmitter;
import debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;

class X86CallEmitterTest {
	public static function run(assert:Assert):Void {
		emitsCdeclIntCall(assert);
		pushesWideDoubleArgumentAsTwoDwords(assert);
		spillsFloatReturnToScratch(assert);
		rejectsTooManyArguments(assert);
	}

	static function hex(bytes:Bytes):String {
		var s = new StringBuf();
		for (i in 0...bytes.length) {
			s.add(StringTools.hex(bytes.get(i), 2));
		}
		return s.toString().toLowerCase();
	}

	static function emitsCdeclIntCall(assert:Assert):Void {
		// add(20, 3): push args right-to-left (3 then 20), cdecl cleanup of 8 bytes
		var bytes = new X86CallEmitter().build(Int64.ofInt(0x1000),
			[{isFloat: false, bits: Int64.ofInt(20)}, {isFloat: false, bits: Int64.ofInt(3)}], 0);
		var h = hex(bytes);
		assert.isTrue(StringTools.startsWith(h, "5152"), "saves ecx then edx");
		assert.isTrue(h.indexOf("6803000000") >= 0, "pushes the second arg (3) first");
		assert.isTrue(h.indexOf("6814000000") >= 0, "pushes the first arg (20) last");
		assert.isTrue(h.indexOf("b800100000") >= 0, "mov eax, funcAddr");
		assert.isTrue(h.indexOf("ffd0") >= 0, "call eax");
		assert.isTrue(h.indexOf("81c408000000") >= 0, "add esp, 8 (cdecl cleanup of two dwords)");
		assert.isTrue(StringTools.endsWith(h, "5a59cc"), "restores edx, ecx then int3");
	}

	static function pushesWideDoubleArgumentAsTwoDwords(assert:Assert):Void {
		// a double (1.0 = 0x3FF0000000000000): high then low so low lands lower
		var arg:CallArg = {isFloat: true, bits: Int64.make(0x3FF00000, 0x00000000), wide: true};
		var bytes = new X86CallEmitter().build(Int64.ofInt(0x2000), [arg], 0);
		var h = hex(bytes);
		// high pushed first (68 00 00 f0 3f), then low (68 00 00 00 00) at a lower
		// address, so the qword reads back little-endian
		assert.isTrue(h.indexOf("680000f03f6800000000") >= 0, "pushes high dword then low dword");
		assert.isTrue(h.indexOf("81c408000000") >= 0, "cleans up 8 bytes for the double");
	}

	static function spillsFloatReturnToScratch(assert:Assert):Void {
		var f64 = hex(new X86CallEmitter().build(Int64.ofInt(0x1000), [], 64));
		var f32 = hex(new X86CallEmitter().build(Int64.ofInt(0x1000), [], 32));
		var intRet = hex(new X86CallEmitter().build(Int64.ofInt(0x1000), [], 0));
		assert.isTrue(f64.indexOf("dd5c2408") >= 0, "F64 return spills ST0 as a qword to [esp+8]");
		assert.isTrue(f32.indexOf("d95c2408") >= 0, "F32 return spills ST0 as a dword to [esp+8]");
		assert.isTrue(intRet.indexOf("dd5c2408") < 0 && intRet.indexOf("d95c2408") < 0,
			"an int return spills nothing");
	}

	static function rejectsTooManyArguments(assert:Assert):Void {
		var many:Array<CallArg> = [for (i in 0...17) {isFloat: false, bits: Int64.ofInt(i)}];
		var threw = false;
		try {
			new X86CallEmitter().build(Int64.ofInt(1), many, 0);
		} catch (e:debug.DebugError) {
			threw = true;
		}
		assert.isTrue(threw, "rejects more than the supported argument count");
	}
}
