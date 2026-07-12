package tests;

import debug.eval.CallEmitter;

import haxe.Int64;
import haxe.io.Bytes;

class CallEmitterTest {
	public static function run(assert:Assert):Void {
		emitsWin64IntCall(assert);
		placesFloatArgumentInXmm(assert);
		rejectsTooManyArguments(assert);
		capturesFloatReturn(assert);
	}

	static function hex(bytes:Bytes):String {
		var s = new StringBuf();
		for (i in 0...bytes.length) {
			s.add(StringTools.hex(bytes.get(i), 2));
		}
		return s.toString().toLowerCase();
	}

	static function emitsWin64IntCall(assert:Assert):Void {
		// call f(5) at 0x1122334455667788, integer return, Windows x64
		var bytes = new CallEmitter(true).build(
			Int64.make(0x11223344, 0x55667788),
			[{isFloat: false, bits: Int64.ofInt(5)}],
			false);
		var h = hex(bytes);

		// exact size (see the byte accounting in the emitter): 151 bytes
		assert.equals(151, bytes.length, "trampoline size for one int arg");
		// starts by saving RCX: push rcx = 0x51
		assert.equals("51", h.substr(0, 2), "prologue saves RCX first (push rcx)");
		// the argument load: mov rcx, 5  = 48 b9 05 00 00 00 00 00 00 00
		assert.isTrue(h.indexOf("48b90500000000000000") >= 0, "arg 0 loaded into RCX (mov rcx, 5)");
		// the call target: mov rax, 0x1122334455667788 (little-endian) ; call rax
		assert.isTrue(h.indexOf("48b88877665544332211ffd0") >= 0, "mov rax, addr ; call rax");
		// traps on return
		assert.equals("cc", h.substr(h.length - 2), "ends with int3");
	}

	static function placesFloatArgumentInXmm(assert:Assert):Void {
		// f(3, 1.5): arg 0 int -> RCX, arg 1 float -> XMM1 (positional on win64)
		var bytes = new CallEmitter(true).build(
			Int64.ofInt(0x400000),
			[{isFloat: false, bits: Int64.ofInt(3)}, {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(1.5)}],
			false);
		var h = hex(bytes);
		// XMM1 loaded via RAX + an 8-byte stack slot: push rax (50) ; movsd
		// xmm1,[rsp] (f20f100c24) ; add rsp,8 (4883c408) — the balanced pop
		assert.isTrue(h.indexOf("50f20f100c244883c408") >= 0, "float arg staged into XMM1 with a balanced 8-byte pop");
		// the int arg still lands in RCX
		assert.isTrue(h.indexOf("48b90300000000000000") >= 0, "int arg 0 in RCX (mov rcx, 3)");
	}

	static function rejectsTooManyArguments(assert:Assert):Void {
		var tooMany = [for (i in 0...5) {isFloat: false, bits: Int64.ofInt(i)}];
		var threw = false;
		try {
			new CallEmitter(true).build(Int64.ofInt(1), tooMany, false);
		} catch (e:debug.DebugError) {
			threw = true;
		}
		assert.isTrue(threw, "win64 rejects a 5th register argument");
	}

	static function capturesFloatReturn(assert:Assert):Void {
		var intRet = new CallEmitter(true).build(Int64.ofInt(0x1000), [], false);
		var floatRet = new CallEmitter(true).build(Int64.ofInt(0x1000), [], true);
		// the float-return path adds the XMM0->RAX capture (movsd [rsp],xmm0 = f20f110424)
		assert.isTrue(hex(floatRet).indexOf("f20f110424") >= 0, "float return captures XMM0");
		assert.isTrue(floatRet.length > intRet.length, "float return emits extra capture code");
	}
}
