package tests;

import debug.Pointer;
import debug.layout.Align;
import debug.target.MemoryReader;
import debug.target.ThreadRegistry;

import haxe.Int64;

class ThreadRegistryTest {
	public static function run(assert:Assert):Void {
		singleThreadWhenNotThreaded(assert);
		readsRegistryWithNamesAndMainByLowestId(assert);
		skipsInvisibleThreads(assert);
		implausibleCountFallsBack(assert);
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
		pokeI32(api, at, value);
		pokeI32(api, at + 4, 0);
	}

	static function pokeName(api:FakeDebugApi, at:Int, name:String):Void {
		for (i in 0...name.length) {
			api.poke(addr(at + i), name.charCodeAt(i));
		}
		api.poke(addr(at + name.length), 0);
	}

	static function registry(api:FakeDebugApi):ThreadRegistry {
		// 64-bit, HL 1.15 (name field present)
		return new ThreadRegistry(new MemoryReader(api, 1, true), new Align(true, false), 1, 15);
	}

	static final PTR = 8;
	static final FLAGS = PTR * 6 + 8; // 56
	static final NAME = FLAGS + 8; // 64

	static function singleThreadWhenNotThreaded(assert:Assert):Void {
		var api = new FakeDebugApi();
		var threads = registry(api).read(addr(0), false, 4242);
		assert.equals(1, threads.length, "one synthesized thread");
		assert.equals(4242, threads[0].id, "uses the stopped id");
		assert.equals("main", threads[0].name, "the lone thread is main");
	}

	static function readsRegistryWithNamesAndMainByLowestId(assert:Assert):Void {
		var api = new FakeDebugApi();
		// registry @0x100: count=2 @0, array ptr @+8 -> 0x200
		pokeI32(api, 0x100, 2);
		pokePtr(api, 0x108, 0x200);
		// array @0x200: [tinfo0 -> 0x300, tinfo1 -> 0x400]
		pokePtr(api, 0x200, 0x300);
		pokePtr(api, 0x208, 0x400);
		// tinfo0 @0x300: tid=77 (the HIGHER id), flags=0, name="worker"
		pokeI32(api, 0x300, 77);
		pokeI32(api, 0x300 + FLAGS, 0);
		pokeName(api, 0x300 + NAME, "worker");
		// tinfo1 @0x400: tid=5 (the LOWER id), flags=0, no name
		pokeI32(api, 0x400, 5);
		pokeI32(api, 0x400 + FLAGS, 0);

		var threads = registry(api).read(addr(0x100), true, 77);
		assert.equals(2, threads.length, "two visible threads");
		// sorted by id: 5 first (main, unnamed), then 77 (worker, named)
		assert.equals(5, threads[0].id, "lowest id first");
		assert.equals("main", threads[0].name, "lowest id is main even though we stopped in 77");
		assert.equals(77, threads[1].id, "higher id second");
		assert.equals("worker", threads[1].name, "explicit name kept");
	}

	static function skipsInvisibleThreads(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 2);
		pokePtr(api, 0x108, 0x200);
		pokePtr(api, 0x200, 0x300);
		pokePtr(api, 0x208, 0x400);
		// visible user thread
		pokeI32(api, 0x300, 9);
		pokeI32(api, 0x300 + FLAGS, 0);
		// invisible (flag bit 16) internal thread -> skipped
		pokeI32(api, 0x400, 3);
		pokeI32(api, 0x400 + FLAGS, 16);

		var threads = registry(api).read(addr(0x100), true, 9);
		assert.equals(1, threads.length, "invisible thread hidden");
		assert.equals(9, threads[0].id, "only the visible thread remains");
	}

	static function implausibleCountFallsBack(assert:Assert):Void {
		var api = new FakeDebugApi();
		pokeI32(api, 0x100, 999999); // garbage count
		var threads = registry(api).read(addr(0x100), true, 88);
		assert.equals(1, threads.length, "garbage count falls back to one thread");
		assert.equals(88, threads[0].id, "fallback uses the stopped id");
	}
}
