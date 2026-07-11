package tests;

import debug.layout.Align;
import debug.layout.FieldLayout;
import debug.layout.ObjectLayout;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

class ObjectLayoutTest {
	public static function run(assert:Assert):Void {
		fieldsAfterHeaderAligned(assert);
		superclassFieldsComeFirst(assert);
		structLayoutHasNoHeader(assert);
		packedFieldInlinesStruct(assert);
		packedAlignsOnSubLargestField(assert);
	}

	static function proto(name:String, tsuper:Null<HLType>, fields:Array<{name:String, t:HLType}>):ObjPrototype {
		return {name: name, tsuper: tsuper, fields: fields, proto: [], globalValue: null, bindings: []};
	}

	static function offsetOf(layout:Array<debug.layout.FieldLayout>, name:String):Int {
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

	static function structLayoutHasNoHeader(assert:Assert):Void {
		var vec = proto("Vec2", null, [{name: "x", t: HF64}, {name: "y", t: HF64}]);
		var layout = new ObjectLayout(new Align(true, false)).fields(vec, true);
		assert.equals(0, offsetOf(layout, "x"), "struct first field at 0 (no hl_type* header)");
		assert.equals(8, offsetOf(layout, "y"), "struct second field packed after the first");
	}

	static function packedFieldInlinesStruct(assert:Assert):Void {
		// Holder { id:i32, pos:@:packed Vec2{f64,f64}, tail:i32 }: header 8,
		// id@8 (size 12), pos aligned on Vec2's largest field (8) -> @16,
		// occupying Vec2's full 16 bytes, tail right after -> @32
		var vec = proto("Vec2", null, [{name: "x", t: HF64}, {name: "y", t: HF64}]);
		var holder = proto("Holder", null, [
			{name: "id", t: HI32},
			{name: "pos", t: HPacked({v: HStruct(vec)})},
			{name: "tail", t: HI32},
		]);
		var layout = new ObjectLayout(new Align(true, false)).fields(holder);
		assert.equals(8, offsetOf(layout, "id"), "id after the header");
		assert.equals(16, offsetOf(layout, "pos"), "packed struct aligned on its largest field");
		assert.equals(32, offsetOf(layout, "tail"), "tail after the inlined struct size");
	}

	static function packedAlignsOnSubLargestField(assert:Assert):Void {
		// sub{a:i32} has largestField 4: after b (a byte @8, size 9) the packed
		// field aligns to 12, not to a pointer boundary
		var sub = proto("Tiny", null, [{name: "a", t: HI32}]);
		var holder = proto("ByteHolder", null, [
			{name: "b", t: HUi8},
			{name: "pos", t: HPacked({v: HStruct(sub)})},
		]);
		var layout = new ObjectLayout(new Align(true, false)).fields(holder);
		assert.equals(8, offsetOf(layout, "b"), "byte field after the header");
		assert.equals(12, offsetOf(layout, "pos"), "packed field aligned to the sub-struct's i32");
	}
}
