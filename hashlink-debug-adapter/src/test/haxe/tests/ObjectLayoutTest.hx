package tests;

import debug.Align;
import debug.ObjectLayout;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

class ObjectLayoutTest {
	public static function run(assert:Assert):Void {
		fieldsAfterHeaderAligned(assert);
		superclassFieldsComeFirst(assert);
	}

	static function proto(name:String, tsuper:Null<HLType>, fields:Array<{name:String, t:HLType}>):ObjPrototype {
		return {name: name, tsuper: tsuper, fields: fields, proto: [], globalValue: null, bindings: []};
	}

	static function offsetOf(layout:Array<debug.FieldLayout>, name:String):Int {
		for (f in layout) {
			if (f.name == name) {
				return f.offset;
			}
		}
		return -1;
	}

	static function fieldsAfterHeaderAligned(assert:Assert):Void {
		// Point { x:i32, y:i32, label:pointer } — like the fixture
		var pointType = HObj({name: "String", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		var point = proto("Point", null, [{name: "x", t: HI32}, {name: "y", t: HI32}, {name: "label", t: pointType}]);
		var layout = new ObjectLayout(new Align(true, false)).fields(point);
		assert.equals(8, offsetOf(layout, "x"), "x after 8-byte header");
		assert.equals(12, offsetOf(layout, "y"), "y after x");
		assert.equals(16, offsetOf(layout, "label"), "label (pointer) at 16");
	}

	static function superclassFieldsComeFirst(assert:Assert):Void {
		var base = proto("Base", null, [{name: "a", t: HI32}]); // a@8
		var sub = proto("Sub", HObj(base), [{name: "b", t: HF64}]); // header+a = 12, pad to 8 -> b@16
		var layout = new ObjectLayout(new Align(true, false)).fields(sub);
		assert.equals(8, offsetOf(layout, "a"), "inherited field keeps its offset");
		assert.equals(16, offsetOf(layout, "b"), "subclass f64 field aligned after parent");
	}
}
