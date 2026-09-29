package ijhaxe.debug.target;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;

import haxe.Int64;

/**
	Reads the live thread list from HashLink's runtime thread registry (the
	`threadsPtr` sent in the handshake), a port of hld `Debugger.readThreads`.

	The program's `threads` compile flag decides the format:
	 - Without thread support there is no registry. The process has a single
	   thread, reported as one entry with the stopped thread's id.
	 - With thread support, the registry is a `count` (i32 @ +0) followed by the
	   `hl_thread_info*` array (`Align.threadsArray`). Each info holds the OS tid
	   @ +0, a flags word (`Align.threadFlags`; flag value 16 marks an invisible
	   thread, which is skipped) and, only on HL runtime 1.13 and later, a
	   128-byte UTF-8 name eight bytes past the flags.

	All architecture-dependent offsets come from the `ijhaxe.debug.layout.Align`
	descriptor; a wrong offset reads plausible garbage. The name offset also
	depends on the runtime version.

	"main" is the lowest thread id, not the thread of the stop: a stop can land
	in any thread.
**/
class ThreadRegistry {
	static inline var FLAG_INVISIBLE = 16;
	static inline var MAX_THREADS = 4096; // sanity cap; a bad count reads as garbage
	static inline var NAME_BYTES = 128;

	final mem:MemoryReader;
	final align:Align;
	final hlVersion:Float; // major + minor/100, for the >= 1.13 name-offset branch

	public function new(mem:MemoryReader, align:Align, hlVersionMajor:Int, hlVersionMinor:Int) {
		this.mem = mem;
		this.align = align;
		this.hlVersion = hlVersionMajor + hlVersionMinor / 100;
	}

	/**
		The live threads. `threadsEnabled` is the handshake's `threads` flag.
		`stoppedThreadId` is the fallback when there is no registry (a
		single-threaded program) or the registry cannot be read.
	**/
	public function read(registryPtr:Pointer, threadsEnabled:Bool, stoppedThreadId:Int):Array<ThreadInfo> {
		var raw:Array<{id:Int, name:Null<String>}> = threadsEnabled && !registryPtr.isNull()
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
		var array = mem.readPointer(registryPtr.offset(align.threadsArray));
		if (array.isNull()) {
			return [];
		}
		// thread_name[128] follows the flags and exc_stack_count, two i32s, so it
		// is +8 on both bitnesses; it exists only on HL runtime 1.13 and later
		var namePos = hlVersion >= 1.13 ? align.threadFlags + 8 : -1;
		var result:Array<{id:Int, name:Null<String>}> = [];
		for (i in 0...count) {
			var info = mem.readPointer(array.offset(align.ptr * i));
			if (info.isNull()) {
				continue;
			}
			if (mem.readI32(info.offset(align.threadFlags)) & FLAG_INVISIBLE != 0) {
				continue; // GC / internal thread, hidden from the user
			}
			var id = mem.readI32(info);
			var name = namePos >= 0 ? readName(info.offset(namePos)) : null;
			result.push({id: id, name: name});
		}
		return result;
	}

	// Reads a fixed-size, NUL-terminated UTF-8 name buffer; null when empty.
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
}
