package tests;

import debug.Align;
import debug.GlobalTable;
import format.hl.Data.HLType;

class GlobalTableTest {
	public static function run(assert:Assert):Void {
		offsetsAlignedInIndexOrder(assert);
	}

	static function offsetsAlignedInIndexOrder(assert:Assert):Void {
		// globals: [i32, f64, bool, ptr] on 64-bit with 1-byte bools
		var table = new GlobalTable(new Align(true, false),
			[HI32, HF64, HBool, HObj({name: "X", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []})]);
		assert.equals(0, table.offsetOf(0), "first global at 0");
		assert.equals(8, table.offsetOf(1), "f64 aligned to 8 after i32");
		assert.equals(16, table.offsetOf(2), "bool after f64");
		assert.equals(24, table.offsetOf(3), "pointer aligned to 8 after bool");
	}
}
