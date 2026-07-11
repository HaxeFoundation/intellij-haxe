package tests;

import debug.Pointer;
import debug.layout.Align;
import debug.target.MemoryReader;
import debug.values.MapKeyKind;
import debug.values.MapReader;
import haxe.Int64;

/**
 * Fabricated-memory test for the native map walk, focused on the Int64Map key
 * layout (8-byte keys) — which cannot be exercised end-to-end because
 * hl.types.Int64Map is not constructible from ordinary Haxe. The String/Int/
 * Object flavours are covered by VariablesIntegrationTest against a real map.
 */
class MapReaderTest {
	public static function run(assert:Assert):Void {
		walksInt64SmallMap(assert);
		unsupportedRuntimeYieldsNoCount(assert);
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

	static function pokeI64(api:FakeDebugApi, at:Int, low:Int):Void {
		pokePtr(api, at, low); // high bytes zero
	}

	static function reader(api:FakeDebugApi, supported:Bool):MapReader {
		return new MapReader(new MemoryReader(api, 1, true), new Align(true, false), supported);
	}

	// native map @0x1000, one int64 entry key=42; small map -> byte cells/nexts
	static function walksInt64SmallMap(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokePtr(api, 0x1000, 0x2000); // cells
		pokePtr(api, 0x1008, 0x2100); // nexts
		pokePtr(api, 0x1010, 0x2200); // entries (keys)
		pokePtr(api, 0x1018, 0x2300); // values
		// counts at ptr*4 + (ptr + 4 + 4) = 48
		pokeI32(api, 0x1000 + 48, 1); // ncells
		pokeI32(api, 0x1000 + 52, 1); // nentries
		pokeI32(api, 0x1000 + 56, 1); // maxEntries (<128 -> small)
		api.poke(addr(0x2000), 0); // cells[0] = entry 0
		api.poke(addr(0x2100), 255); // nexts[0] = end
		pokeI64(api, 0x2200, 42); // entries[0] = key 42
		pokePtr(api, 0x2300, 0xDEAD); // values[0] = value slot

		var map = reader(api, true);
		assert.equals(1, map.entryCount(addr(0x1000)), "one live entry");
		var entries = map.entries(addr(0x1000), Int64Key, _ -> "?");
		assert.equals(1, entries.length, "one entry walked");
		assert.equals("42", entries[0].key, "int64 key decoded");
		assert.isTrue(Int64.eq(entries[0].valueAddress, addr(0x2300)), "value at values[0]");
	}

	static function unsupportedRuntimeYieldsNoCount(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokePtr(api, 0x900, 0x1000);
		assert.equals(-1, reader(api, false).entryCount(addr(0x1000)), "pre-1.13 layout not supported");
	}
}
