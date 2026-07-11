package tests;

import debug.layout.Align;
import debug.layout.EnumLayout;

import format.hl.Data.EnumPrototype;
import format.hl.Data.HLType;

class EnumLayoutTest {
	public static function run(assert:Assert):Void {
		paramsPackIntoHeaderPadding(assert);
		floatParamAlignsToEight(assert);
		noParamsYieldsEmpty(assert);
	}

	// typical x64 C alignments from the handshake: ui8,ui16,i32,i64,f32,f64,bool,ptr
	static function align():Align {
		var a = new Align(true, false);
		a.structSizes = [0, 1, 2, 4, 8, 4, 8, 1, 8];
		return a;
	}

	static function shade():EnumPrototype {
		var stringType = HObj({name: "String", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		return {
			name: "Shade",
			globalValue: null,
			constructs: [{name: "Plain", params: []}, {name: "Tinted", params: [HI32, stringType]}]
		};
	}

	static function paramsPackIntoHeaderPadding(assert:Assert):Void {
		// header: type ptr (8) + index i32 (12); the i32 param packs at +12
		var params = new EnumLayout(align()).params(shade(), 1);
		assert.equals(2, params.length, "Tinted has two params");
		assert.equals(12, params[0].offset, "i32 param packs into the header padding");
		assert.equals(16, params[1].offset, "pointer param aligned to 8 after it");
	}

	static function floatParamAlignsToEight(assert:Assert):Void {
		var proto:EnumPrototype = {
			name: "E",
			globalValue: null,
			constructs: [{name: "C", params: [HF64, HI32]}]
		};
		var params = new EnumLayout(align()).params(proto, 0);
		assert.equals(16, params[0].offset, "f64 param aligns past the 12-byte header");
		assert.equals(24, params[1].offset, "i32 follows the f64");
	}

	static function noParamsYieldsEmpty(assert:Assert):Void {
		assert.equals(0, new EnumLayout(align()).params(shade(), 0).length, "Plain has no params");
		assert.equals(0, new EnumLayout(align()).params(shade(), 99).length, "out-of-range index is safe");
	}
}
