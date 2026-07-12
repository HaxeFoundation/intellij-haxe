package debug.target;

import debug.Pointer;
import debug.layout.Align;

import haxe.Int64;

/**
 * Reads the live thread list from HashLink's runtime thread registry (the
 * `threadsPtr` sent in the handshake), a port of hld `Debugger.readThreads`.
 *
 * Two format cases (the program's `threads` compile flag):
 *  - NOT compiled with thread support: there is no registry to walk; the
 *    process has a single thread — reported as one entry from the stopped id.
 *  - compiled with thread support: the registry is `count` (i32 @ +0) followed
 *    by an array of `hl_thread_info*` @ +ptr; each info has its OS tid @ +0,
 *    a flags word @ `ptr*6 + 8` (bit 16 = invisible, skipped), and — only on
 *    HL runtime >= 1.13 — a 128-byte UTF-8 name @ `ptr*6 + 16`.
 *
 * The registry offsets are the hld layout and MUST be empirically pinned
 * against real HL 1.15 (wrong offsets read plausible garbage). The name offset
 * branches on the runtime version.
 *
 * "main" is the LOWEST thread id (not wherever we happened to stop): a stop can
 * land in any thread, so tying the name to the stopped thread would be wrong.
 */
class ThreadRegistry {
	static inline var FLAG_INVISIBLE = 16;
	static inline var MAX_THREADS = 4096; // sanity cap; a bad count reads as garbage
	static inline var NAME_BYTES = 128;

	final mem:MemoryReader;
	final ptr:Int;
	final hlVersion:Float; // major + minor/100, for the >= 1.13 name-offset branch

	public function new(mem:MemoryReader, align:Align, hlVersionMajor:Int, hlVersionMinor:Int) {
		this.mem = mem;
		this.ptr = align.ptr;
		this.hlVersion = hlVersionMajor + hlVersionMinor / 100;
	}

	/**
	 * The live threads. `threadsEnabled` is the handshake `threads` flag;
	 * `stoppedThreadId` is the fallback when there is no registry (single-thread
	 * program) or the registry is unreadable.
	 */
	public function read(registryPtr:Pointer, threadsEnabled:Bool, stoppedThreadId:Int):Array<ThreadInfo> {
		var raw:Array<{id:Int, name:Null<String>}> = threadsEnabled && !isNull(registryPtr)
			? readRegistry(registryPtr)
			: [];
		if (raw.length == 0) {
			raw.push({id: stoppedThreadId, name: null});
		}
		// lowest id is "main" when unnamed; other unnamed threads get "thread-<id>"
		raw.sort((a, b) -> a.id - b.id);
		return [
			for (i in 0...raw.length)
				{id: raw[i].id, name: raw[i].name != null ? raw[i].name : (i == 0 ? "main" : "thread-" + raw[i].id)}
		];
	}

	function readRegistry(registryPtr:Pointer):Array<{id:Int, name:Null<String>}> {
		var count = mem.readI32(registryPtr);
		if (count <= 0 || count > MAX_THREADS) {
			return [];
		}
		var array = mem.readPointer(offset(registryPtr, ptr));
		if (isNull(array)) {
			return [];
		}
		var flagsPos = ptr * 6 + 8;
		var namePos = hlVersion >= 1.13 ? flagsPos + 8 : -1;
		var result:Array<{id:Int, name:Null<String>}> = [];
		for (i in 0...count) {
			var info = mem.readPointer(offset(array, ptr * i));
			if (isNull(info)) {
				continue;
			}
			if (mem.readI32(offset(info, flagsPos)) & FLAG_INVISIBLE != 0) {
				continue; // GC / internal thread, hidden from the user
			}
			var id = mem.readI32(info);
			var name = namePos >= 0 ? readName(offset(info, namePos)) : null;
			result.push({id: id, name: name});
		}
		return result;
	}

	// A fixed-size, null-terminated UTF-8 name buffer; null/empty → no name.
	function readName(address:Pointer):Null<String> {
		var bytes = mem.read(address, NAME_BYTES);
		var len = 0;
		while (len < NAME_BYTES && bytes.get(len) != 0) {
			len++;
		}
		if (len == 0) {
			return null;
		}
		return bytes.getString(0, len);
	}

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}

	static inline function isNull(p:Pointer):Bool {
		return Int64.eq(p, Int64.ofInt(0));
	}
}
