package tests;

import debug.Pointer;
import debug.layout.Align;
import debug.layout.ObjectLayout;
import debug.target.MemoryReader;
import debug.values.RuntimeTypes;
import debug.values.ValueChildren;
import debug.values.ValueReader;

import format.hl.Data.HLType;
import haxe.Int64;

class ValueChildrenTest {
	public static function run(assert:Assert):Void {
		listsArrayBytesIntElements(assert);
		listsArrayObjStringElements(assert);
		capsHugeArrays(assert);
		listsStructFieldsAtZeroBase(assert);
		listsPackedFieldAsExpandableStruct(assert);
		listsClosureCapturedValue(assert);
		boundlessClosureHasNoChildren(assert);
	}

	static function addr(v:Int):Pointer {
		return Int64.ofInt(v);
	}

	static function pokeI32(api:FakeDebugApi, at:Int, value:Int):Void {
		for (i in 0...4) {
			api.poke(addr(at + i), (value >>> (8 * i)) & 0xFF);
		}
	}

	static function pokePtr(api:FakeDebugApi, at:Int, value:Int):Void {
		for (i in 0...4) {
			api.poke(addr(at + i), (value >>> (8 * i)) & 0xFF);
		}
		for (i in 4...8) {
			api.poke(addr(at + i), 0);
		}
	}

	static function pokeUcs2(api:FakeDebugApi, at:Int, s:String):Void {
		for (i in 0...s.length) {
			api.poke(addr(at + i * 2), s.charCodeAt(i) & 0xFF);
			api.poke(addr(at + i * 2 + 1), s.charCodeAt(i) >> 8);
		}
		api.poke(addr(at + s.length * 2), 0);
		api.poke(addr(at + s.length * 2 + 1), 0);
	}

	static function children(api:FakeDebugApi, ?resolve:String->Null<HLType>):ValueChildren {
		var mem = new MemoryReader(api, 1, true);
		var align = new Align(true, false);
		var reader = new ValueReader(mem, align);
		reader.referenceAllocator = (_, _) -> 42; // any nonzero: mark expandable
		var vc = new ValueChildren(mem, align, reader, new ObjectLayout(align));
		if (resolve != null) {
			vc.runtimeTypes = new RuntimeTypes(mem, resolve);
		}
		return vc;
	}

	static function arrayBytesIntType():HLType {
		return HObj({name: "hl.types.ArrayBytes_Int", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
	}

	static function listsArrayBytesIntElements(assert:Assert):Void {
		var api = new FakeDebugApi();
		// wrapper @0x1000: header@0, length@8 = 3, bytes@16 -> 0x2000
		pokeI32(api, 0x1008, 3);
		pokePtr(api, 0x1010, 0x2000);
		pokeI32(api, 0x2000, 2);
		pokeI32(api, 0x2004, 5);
		pokeI32(api, 0x2008, 10);

		var vars = children(api).of(addr(0x1000), arrayBytesIntType());
		assert.equals(3, vars.length, "three elements");
		assert.equals("0", vars[0].name, "indexed names");
		assert.equals("2", vars[0].value, "elem 0");
		assert.equals("5", vars[1].value, "elem 1");
		assert.equals("10", vars[2].value, "elem 2");
	}

	static function listsArrayObjStringElements(assert:Assert):Void {
		var api = new FakeDebugApi();
		var stringType = HObj({name: "String", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		// ArrayObj @0x1000: length@8 = 1, native varray@16 -> 0x3000
		pokeI32(api, 0x1008, 1);
		pokePtr(api, 0x1010, 0x3000);
		// varray @0x3000: at@8 -> runtime hl_type @0x4000 (HOBJ "String"), size@16 = 4
		pokePtr(api, 0x3008, 0x4000);
		pokeI32(api, 0x3010, 4);
		// runtime hl_type "String": kind=11 @0x4000, data@0x4008 -> 0x4100, name@0x4110 -> 0x4200
		pokeI32(api, 0x4000, 11);
		pokePtr(api, 0x4008, 0x4100);
		pokePtr(api, 0x4110, 0x4200);
		pokeUcs2(api, 0x4200, "String");
		// element slot 0 at varray+24 -> String object @0x5000 with bytes@0x6000 "ok"
		pokePtr(api, 0x3018, 0x5000);
		pokePtr(api, 0x5008, 0x6000);
		pokeI32(api, 0x5010, 2);
		pokeUcs2(api, 0x6000, "ok");

		var arrayObjType = HObj({name: "hl.types.ArrayObj", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		var vars = children(api, name -> name == "String" ? stringType : null).of(addr(0x1000), arrayObjType);
		assert.equals(1, vars.length, "one live element (varray size is larger)");
		assert.equals("\"ok\"", vars[0].value, "string element decoded via runtime element type");
	}

	static function capsHugeArrays(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x1008, 600); // length beyond the cap
		pokePtr(api, 0x1010, 0x2000);
		var vars = children(api).of(addr(0x1000), arrayBytesIntType());
		assert.equals(513, vars.length, "512 elements + overflow marker");
		assert.equals("…", vars[512].name, "overflow marker present");
	}

	static function pokeF64(api:FakeDebugApi, at:Int, low:Int, high:Int):Void {
		pokeI32(api, at, low);
		pokeI32(api, at + 4, high);
	}

	static function vec2Proto():format.hl.Data.ObjPrototype {
		return {name: "Vec2", tsuper: null, fields: [{name: "x", t: HF64}, {name: "y", t: HF64}], proto: [], globalValue: null, bindings: []};
	}

	static function vec2Struct():HLType {
		return HStruct(vec2Proto());
	}

	static function listsStructFieldsAtZeroBase(assert:Assert):Void {
		var api = new FakeDebugApi();
		// struct @0x2000 without a type header: x = 1.5 @+0, y = 2.5 @+8
		pokeF64(api, 0x2000, 0, 0x3FF80000);
		pokeF64(api, 0x2008, 0, 0x40040000);
		var vars = children(api).of(addr(0x2000), vec2Struct());
		assert.equals(2, vars.length, "two struct fields");
		assert.equals("1.5", vars[0].value, "x read at offset 0");
		assert.equals("2.5", vars[1].value, "y read at offset 8");
	}

	static function listsPackedFieldAsExpandableStruct(assert:Assert):Void {
		var api = new FakeDebugApi();
		// Holder @0x3000: header, id @8 = 7, packed Vec2 inline @16 (x=1.5, y=2.5)
		pokeI32(api, 0x3008, 7);
		pokeF64(api, 0x3010, 0, 0x3FF80000);
		pokeF64(api, 0x3018, 0, 0x40040000);
		var holder = HObj({name: "Holder", tsuper: null, fields: [
			{name: "id", t: HI32},
			{name: "pos", t: HPacked({v: HStruct(vec2Proto())})},
		], proto: [], globalValue: null, bindings: []});
		var vars = children(api).of(addr(0x3000), holder);
		assert.equals(2, vars.length, "id + pos");
		assert.equals("7", vars[0].value, "plain field before the packed one");
		assert.equals("Vec2", vars[1].type, "packed field typed as the inner struct");
		assert.isTrue(vars[1].reference != 0, "packed field is expandable");
	}

	static function listsClosureCapturedValue(assert:Assert):Void {
		var api = new FakeDebugApi();
		// vclosure @0x1000: fun @+8, hasValue @+16 = 1, value @+24 -> 0x5000
		pokePtr(api, 0x1008, 0x9990);
		pokeI32(api, 0x1010, 1);
		pokePtr(api, 0x1018, 0x5000);
		var vars = children(api).of(addr(0x1000), HFun({args: [], ret: HVoid}));
		assert.equals(1, vars.length, "one captured child");
		assert.equals("captured", vars[0].name, "child named captured");
		assert.isTrue(StringTools.startsWith(vars[0].value, "Dynamic @ 0x5000"), "bound value read from +ptr*3 (was " + vars[0].value + ")");
	}

	static function boundlessClosureHasNoChildren(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokePtr(api, 0x1008, 0x9990);
		pokeI32(api, 0x1010, 0); // hasValue = 0: a plain function pointer
		var vars = children(api).of(addr(0x1000), HFun({args: [], ret: HVoid}));
		assert.equals(0, vars.length, "no children without a bound value");
	}
}
