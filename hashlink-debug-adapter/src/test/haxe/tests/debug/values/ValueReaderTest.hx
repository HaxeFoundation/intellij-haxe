package tests.debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.target.MemoryReader;
import debug.values.RuntimeTypes;
import debug.values.ValueReader;

import format.hl.Data.HLType;
import haxe.Int64;
import haxe.io.Bytes;

class ValueReaderTest {
	public static function run(assert:Assert):Void {
		readsPrimitives(assert);
		readsString(assert);
		nullPointerReadsNull(assert);
		rawFallbackForObjects(assert);
		readsNullBox(assert);
		readsDynamicInt(assert);
		readsClosureName(assert);
		readsRefThroughIndirection(assert);
		functionTypeShowsSignature(assert);
	}

	static function addr(v:Int):Pointer {
		return Int64.ofInt(v);
	}

	static function reader(api:FakeDebugApi):ValueReader {
		return new ValueReader(new MemoryReader(api, 1, true), new Align(true, false));
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

	static function readsPrimitives(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 42);
		api.poke(addr(0x200), 1); // bool true
		var floatBytes = Bytes.alloc(8);
		floatBytes.setDouble(0, 3.5);
		for (i in 0...8) {
			api.poke(addr(0x300 + i), floatBytes.get(i));
		}

		var r = reader(api);
		assert.equals("42", r.read(addr(0x100), HI32).value, "i32 value");
		assert.equals("Int", r.read(addr(0x100), HI32).type, "i32 type");
		assert.equals("true", r.read(addr(0x200), HBool).value, "bool value");
		assert.equals("3.5", r.read(addr(0x300), HF64).value, "f64 value");
	}

	static function stringType():HLType {
		return HObj({name: "String", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
	}

	static function readsString(assert:Assert):Void {
		var api = new FakeDebugApi();
		// String object at 0x1000: type@0, bytes ptr@8 -> 0x2000, length@16 = 2
		pokePtr(api, 0x1000, 0xABCD); // type ptr (nonzero)
		pokePtr(api, 0x1008, 0x2000); // bytes ptr
		pokeI32(api, 0x1010, 2); // length (chars)
		// UTF-16 "Hi" at 0x2000
		api.poke(addr(0x2000), 0x48);
		api.poke(addr(0x2001), 0x00);
		api.poke(addr(0x2002), 0x69);
		api.poke(addr(0x2003), 0x00);
		// a slot at 0x900 holding a pointer to the String object
		pokePtr(api, 0x900, 0x1000);

		var decoded = reader(api).read(addr(0x900), stringType());
		assert.equals("\"Hi\"", decoded.value, "string decoded from UTF-16");
		assert.equals("String", decoded.type, "string type");
		assert.equals(0, decoded.reference, "string is a leaf");
	}

	static function nullPointerReadsNull(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokePtr(api, 0x900, 0); // null pointer in the slot
		var decoded = reader(api).read(addr(0x900), stringType());
		assert.equals("null", decoded.value, "null pointer reads as null");
	}

	static function rawFallbackForObjects(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokePtr(api, 0x900, 0x5000); // slot -> some object pointer
		var objType = HObj({name: "Main", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []});
		var decoded = reader(api).read(addr(0x900), objType);
		assert.isTrue(StringTools.startsWith(decoded.value, "Main @ 0x"), "object shows raw type@addr (was " + decoded.value + ")");
		assert.equals(0, decoded.reference, "raw fallback is non-expandable in step 1");
	}

	static function readsNullBox(assert:Assert):Void {
		var api = new FakeDebugApi();
		// slot @0x900 -> box @0x1000: type header @0, boxed i32 @+8 = 7
		pokePtr(api, 0x900, 0x1000);
		pokePtr(api, 0x1000, 0xBEEF);
		pokeI32(api, 0x1008, 7);
		var decoded = reader(api).read(addr(0x900), HNull(HI32));
		assert.equals("7", decoded.value, "Null<Int> box decodes the payload");
		assert.equals("Int", decoded.type, "Null<Int> reads as Int");
	}

	static function readsDynamicInt(assert:Assert):Void {
		var api = new FakeDebugApi();
		// slot @0x900 -> vdynamic @0x1000: runtime type @0 -> hl_type(kind=3 HI32) @0x2000, payload @+8 = 42
		pokePtr(api, 0x900, 0x1000);
		pokePtr(api, 0x1000, 0x2000);
		pokeI32(api, 0x1008, 42);
		pokeI32(api, 0x2000, 3); // kind 3 = HI32
		var r = reader(api);
		r.runtimeTypes = new debug.values.RuntimeTypes(new MemoryReader(api, 1, true), _ -> null);
		var decoded = r.read(addr(0x900), HDyn);
		assert.equals("42", decoded.value, "Dynamic holding an Int decodes via the runtime type");
	}

	static function readsRefThroughIndirection(assert:Assert):Void {
		var api = new FakeDebugApi();
		// slot @0x900 -> ref @0x1000 (the address of the value slot) -> i32 20
		pokePtr(api, 0x900, 0x1000);
		pokeI32(api, 0x1000, 20);
		var decoded = reader(api).read(addr(0x900), HRef(HI32));
		assert.equals("20", decoded.value, "HRef reads the value through the indirection");
		assert.equals("Int", decoded.type, "HRef labels as the inner type");
	}

	static function readsClosureName(assert:Assert):Void {
		var api = new FakeDebugApi();
		// slot @0x900 -> vclosure @0x1000: type @0, fun ptr @+8 = 0x7777
		pokePtr(api, 0x900, 0x1000);
		pokePtr(api, 0x1008, 0x7777);
		var r = reader(api);
		r.functionNameResolver = fun -> Int64.toStr(fun) == Std.string(0x7777) ? "Main.add" : null;
		var decoded = r.read(addr(0x900), HFun({args: [], ret: HVoid}));
		assert.equals("function Main.add", decoded.value, "closure resolves its function name");
		assert.equals("() -> Void", decoded.type, "closure type shows its signature");
	}

	static function functionTypeShowsSignature(assert:Assert):Void {
		assert.equals("() -> Void", ValueReader.typeName(HFun({args: [], ret: HVoid})),
			"no-arg function signature");
		assert.equals("(Int, String) -> Bool", ValueReader.typeName(HFun({args: [HI32, HObj({
			name: "String", tsuper: null, fields: [], proto: [], globalValue: null, bindings: []
		})], ret: HBool})), "argument and return types");
		assert.equals("(Int) -> (Float) -> Int", ValueReader.typeName(HFun({
			args: [HI32], ret: HFun({args: [HF64], ret: HI32})
		})), "nested function types compose");
		assert.equals("(Int) -> Void", ValueReader.typeName(HMethod({args: [HI32], ret: HVoid})),
			"methods format like functions");
	}
}
