package tests;

import debug.Align;
import debug.FrameLayout;
import format.hl.Data.HLType;

class FrameLayoutTest {
	public static function run(assert:Assert):Void {
		alignPrimitiveSizes(assert);
		windowsLocalsAndStackArgs(assert);
		mixedTypeLocals(assert);
		sysvSpillsFirstSixArgs(assert);
	}

	static function alignPrimitiveSizes(assert:Assert):Void {
		var a = new Align(true, false); // 64-bit, bool=1
		assert.equals(4, a.typeSize(HI32), "i32 size");
		assert.equals(8, a.typeSize(HF64), "f64 size");
		assert.equals(1, a.typeSize(HBool), "bool size 1");
		assert.equals(8, a.typeSize(HObj(null)), "pointer type size");
		assert.equals(8, a.stackSize(HI32), "i32 promoted to ptr on stack (64-bit)");
		assert.equals(0, a.padSize(4, HI32), "aligned i32 needs no pad");
		assert.equals(4, a.padSize(4, HF64), "i32-offset f64 pads to 8");
	}

	static function windowsLocalsAndStackArgs(assert:Assert):Void {
		var layout = new FrameLayout(new Align(true, false), true); // Windows x64
		// a function with no args and three i32 locals (like fixture main's total/count/i)
		var slots = layout.registerOffsets([HI32, HI32, HI32], 0);
		assert.equals(-4, slots[0].offset, "first local at ebp-4");
		assert.equals(-8, slots[1].offset, "second local at ebp-8");
		assert.equals(-12, slots[2].offset, "third local at ebp-12");

		// a function with two i32 args (like fixture add(current, amount))
		var addSlots = layout.registerOffsets([HI32, HI32], 2);
		assert.equals(16, addSlots[0].offset, "first stack arg at ebp+16 (skips rbp+retaddr)");
		assert.equals(24, addSlots[1].offset, "second stack arg at ebp+24");
	}

	static function mixedTypeLocals(assert:Assert):Void {
		var layout = new FrameLayout(new Align(true, false), true);
		// i32 then f64: i32 -> -4; f64 needs 8-alignment: size 4 -> +8 = 12, pad (-12)&7=4 -> 16 -> -16
		var slots = layout.registerOffsets([HI32, HF64], 0);
		assert.equals(-4, slots[0].offset, "i32 local at -4");
		assert.equals(-16, slots[1].offset, "f64 local aligned to -16");
	}

	static function sysvSpillsFirstSixArgs(assert:Assert):Void {
		var layout = new FrameLayout(new Align(true, false), false); // System V (non-Windows) 64-bit
		// two i32 args: both within the first 6 integer registers -> spilled to locals (negative)
		var slots = layout.registerOffsets([HI32, HI32], 2);
		assert.equals(-4, slots[0].offset, "first int arg spilled to ebp-4");
		assert.equals(-8, slots[1].offset, "second int arg spilled to ebp-8");
	}
}
