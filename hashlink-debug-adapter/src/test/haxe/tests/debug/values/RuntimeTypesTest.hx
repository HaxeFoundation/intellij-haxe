package tests.debug.values;

import debug.Pointer;
import debug.target.MemoryReader;
import debug.values.RuntimeTypes;

import format.hl.Data.HLType;
import haxe.Int64;

class RuntimeTypesTest {
	public static function run(assert:Assert):Void {
		primitiveKindsMapDirectly(assert);
		objectKindResolvesByName(assert);
		nullKindWrapsInner(assert);
		unknownReturnsNull(assert);
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

	static function types(api:FakeDebugApi, resolve:String->Null<HLType>):RuntimeTypes {
		return new RuntimeTypes(new MemoryReader(api, 1, true), resolve);
	}

	static function primitiveKindsMapDirectly(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 3); // kind 3 = HI32
		var t = types(api, _ -> null).typeAt(addr(0x100));
		assert.isTrue(t != null && t.match(HI32), "kind 3 resolves to HI32");
	}

	static function objectKindResolvesByName(assert:Assert):Void {
		var api = new FakeDebugApi();
		// hl_type @0x200: kind=11 (HOBJ), data ptr @0x208 -> 0x300
		pokeI32(api, 0x200, 11);
		pokePtr(api, 0x208, 0x300);
		// hl_type_obj @0x300: name ptr @ +16 -> 0x400
		pokePtr(api, 0x310, 0x400);
		pokeUcs2(api, 0x400, "Point");

		var pointType = HObj({name: "Point", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		var resolved = types(api, name -> name == "Point" ? pointType : null).typeAt(addr(0x200));
		assert.isTrue(resolved == pointType, "HOBJ kind resolves Point by runtime name");
	}

	static function nullKindWrapsInner(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 3); // HI32
		pokeI32(api, 0x500, 19); // HNULL
		pokePtr(api, 0x508, 0x100); // wraps the i32 type
		var t = types(api, _ -> null).typeAt(addr(0x500));
		assert.isTrue(t != null && t.match(HNull(HI32)), "HNULL wraps the inner runtime type");
	}

	static function unknownReturnsNull(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 20); // HMETHOD: unsupported
		assert.isTrue(types(api, _ -> null).typeAt(addr(0x100)) == null, "unsupported kind yields null");
		assert.isTrue(types(api, _ -> null).typeAt(addr(0)) == null, "null pointer yields null");
		pokeI32(api, 0x200, 16); // HDYNOBJ
		var dynObj = types(api, _ -> null).typeAt(addr(0x200));
		assert.isTrue(dynObj != null && dynObj.match(HDynObj), "dynobj kind resolves to HDynObj");
		abstractResolvesItsName(assert);
		guidReadsAsInt64(assert);
	}

	static function abstractResolvesItsName(assert:Assert):Void {
		var api = new FakeDebugApi();
		// hl_type @0x300: kind=17 (HABSTRACT); for abstracts the data pointer IS
		// the uchar* name — @0x308 -> 0x400 "hl_bytes_map"
		pokeI32(api, 0x300, 17);
		pokePtr(api, 0x308, 0x400);
		pokeUcs2(api, 0x400, "hl_bytes_map");
		var resolved = types(api, _ -> null).typeAt(addr(0x300));
		assert.isTrue(resolved != null && resolved.match(HAbstract("hl_bytes_map")),
			"abstract kind resolves its name (was " + Std.string(resolved) + ")");
	}

	static function guidReadsAsInt64(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x500, 23); // HGUID: stored as an i64; the format lib has no HGUID
		var resolved = types(api, _ -> null).typeAt(addr(0x500));
		assert.isTrue(resolved != null && resolved.match(HI64), "guid kind reads as Int64");
	}
}
