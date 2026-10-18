package tests.debug.values;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.values.RuntimeTypes;

import format.hl.Data.HLType;
import haxe.Int64;

class RuntimeTypesTest {
	public static function run(assert:Assert):Void {
		primitiveKindsMapDirectly(assert);
		objectKindResolvesByName(assert);
		nullKindWrapsInner(assert);
		dynObjKindResolvesDirectly(assert);
		abstractResolvesItsName(assert);
		guidReadsAsInt64(assert);
		unknownReturnsNull(assert);
		funKindReconstructsTheSignature(assert);
	}

	static function funKindReconstructsTheSignature(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x100, 3); // HI32
		// hl_type @0x600: kind=10 (HFUN), data @0x608 -> hl_type_fun @0x700
		api.pokeI32(0x600, 10);
		api.pokePtr(0x608, 0x700);
		// hl_type_fun: args (hl_type**) @ +0 -> 0x800, ret @ +8 -> i32, nargs @ +16
		api.pokePtr(0x700, 0x800);
		api.pokePtr(0x708, 0x100);
		api.pokeI32(0x710, 1);
		api.pokePtr(0x800, 0x100); // arg[0] = i32

		var resolved = types(api, _ -> null).typeAt(addr(0x600));
		var ok = switch (resolved) {
			case HFun(f) if (f != null): f.args.length == 1 && f.args[0].match(HI32) && f.ret.match(HI32);
			default: false;
		}
		assert.isTrue(ok, "fun kind reconstructs (Int) -> Int (was " + Std.string(resolved) + ")");
	}

	static function addr(v:Int):Pointer {
		return Int64.ofInt(v);
	}

	static function types(api:FakeDebugApi, resolve:String->Null<HLType>):RuntimeTypes {
		return new RuntimeTypes(new MemoryReader(api, 1, true), new Align(true, false), resolve);
	}

	static function primitiveKindsMapDirectly(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x100, 3); // kind 3 = HI32
		var t = types(api, _ -> null).typeAt(addr(0x100));
		assert.isTrue(t != null && t.match(HI32), "kind 3 resolves to HI32");
	}

	static function objectKindResolvesByName(assert:Assert):Void {
		var api = new FakeDebugApi();
		// hl_type @0x200: kind=11 (HOBJ), data ptr @0x208 -> 0x300
		api.pokeI32(0x200, 11);
		api.pokePtr(0x208, 0x300);
		// hl_type_obj @0x300: name ptr @ +16 -> 0x400
		api.pokePtr(0x310, 0x400);
		api.pokeUcs2(0x400, "Point");

		var pointType = HObj({name: "Point", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		var resolved = types(api, name -> name == "Point" ? pointType : null).typeAt(addr(0x200));
		assert.isTrue(resolved == pointType, "HOBJ kind resolves Point by runtime name");
	}

	static function nullKindWrapsInner(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x100, 3); // HI32
		api.pokeI32(0x500, 19); // HNULL
		api.pokePtr(0x508, 0x100); // wraps the i32 type
		var t = types(api, _ -> null).typeAt(addr(0x500));
		assert.isTrue(t != null && t.match(HNull(HI32)), "HNULL wraps the inner runtime type");
	}

	static function dynObjKindResolvesDirectly(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x200, 16); // HDYNOBJ
		var dynObj = types(api, _ -> null).typeAt(addr(0x200));
		assert.isTrue(dynObj != null && dynObj.match(HDynObj), "dynobj kind resolves to HDynObj");
	}

	static function unknownReturnsNull(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x100, 20); // HMETHOD: unsupported
		assert.isTrue(types(api, _ -> null).typeAt(addr(0x100)) == null, "unsupported kind yields null");
		assert.isTrue(types(api, _ -> null).typeAt(addr(0)) == null, "null pointer yields null");
	}

	static function abstractResolvesItsName(assert:Assert):Void {
		var api = new FakeDebugApi();
		// hl_type @0x300: kind=17 (HABSTRACT); for abstracts the data pointer IS
		// the uchar* name — @0x308 -> 0x400 "hl_bytes_map"
		api.pokeI32(0x300, 17);
		api.pokePtr(0x308, 0x400);
		api.pokeUcs2(0x400, "hl_bytes_map");
		var resolved = types(api, _ -> null).typeAt(addr(0x300));
		assert.isTrue(resolved != null && resolved.match(HAbstract("hl_bytes_map")),
			"abstract kind resolves its name (was " + Std.string(resolved) + ")");
	}

	static function guidReadsAsInt64(assert:Assert):Void {
		var api = new FakeDebugApi();
		api.pokeI32(0x500, 23); // HGUID: stored as an i64; the format lib has no HGUID
		var resolved = types(api, _ -> null).typeAt(addr(0x500));
		assert.isTrue(resolved != null && resolved.match(HI64), "guid kind reads as Int64");
	}
}
