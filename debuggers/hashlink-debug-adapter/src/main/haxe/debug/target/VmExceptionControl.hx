package debug.target;

import debug.Pointer;
import debug.layout.Align;

/**
	Drives HashLink's built-in break-on-throw support. hl_throw (src/std/error.c)
	stores the thrown vdynamic in hl_thread_info.exc_value and THEN — when
	HL_EXC_CATCH_ALL is set in that thread's flags — executes hl_debug_break()
	with HL_EXC_IS_THROW set: an int3 the debugger receives at a point where the
	thrown value is finally readable (it is unreadable at hl_throw's ENTRY — the
	argument registers are not exposed by HL's debug API).

	All struct offsets come from the `debug.layout.Align` arch descriptor
	(thread id @ +0, exc_value/flags in the ptr-relative tail); the flags word is
	an i32 whose two bits both live in its first little-endian byte.
**/
class VmExceptionControl {
	static inline var HL_EXC_CATCH_ALL = 2;
	static inline var HL_EXC_IS_THROW = 4;
	static inline var MAX_THREADS = 4096; // sanity cap; a bad count reads as garbage

	final api:DebugApi;
	final pid:Int;
	final mem:MemoryReader;
	final align:Align;
	final registryPtr:Pointer;

	public function new(api:DebugApi, pid:Int, mem:MemoryReader, align:Align, registryPtr:Pointer) {
		this.api = api;
		this.pid = pid;
		this.mem = mem;
		this.align = align;
		this.registryPtr = registryPtr;
	}

	/**
		Requests hl_throw's own debug break for the next throw on `threadId`
		(sets HL_EXC_CATCH_ALL). False when the thread's info cannot be found —
		the caller must then fall back to reporting without the thrown value.
	**/
	public function armCatchAll(threadId:Int):Bool {
		var info = infoFor(threadId);
		if (info == null) {
			return false;
		}
		writeFlagsByte(info, readFlagsByte(info) | HL_EXC_CATCH_ALL);
		return true;
	}

	public function disarmCatchAll(threadId:Int):Void {
		var info = infoFor(threadId);
		if (info != null) {
			writeFlagsByte(info, readFlagsByte(info) & ~HL_EXC_CATCH_ALL);
		}
	}

	/**
		True while `threadId` is parked at hl_throw's own break (HL_EXC_IS_THROW).
	**/
	public function isThrowBreak(threadId:Int):Bool {
		var info = infoFor(threadId);
		return info != null && readFlagsByte(info) & HL_EXC_IS_THROW != 0;
	}

	/**
		The thrown vdynamic* parked in exc_value, or null when unavailable.
	**/
	public function thrownValue(threadId:Int):Null<Pointer> {
		var info = infoFor(threadId);
		if (info == null) {
			return null;
		}
		var value = mem.readPointer(info.offset(align.threadExcValue));
		return value.isNull() ? null : value;
	}

	// hl_thread_info* of the thread with OS id `threadId`, walking the runtime
	// registry (count @ +0, hl_thread_info* array @ +ptr). Null when the
	// registry is absent/unreadable or the thread is not in it.
	function infoFor(threadId:Int):Null<Pointer> {
		if (registryPtr.isNull()) {
			return null;
		}
		var count = mem.readI32(registryPtr);
		if (count <= 0 || count > MAX_THREADS) {
			return null;
		}
		var array = mem.readPointer(registryPtr.offset(align.threadsArray));
		if (array.isNull()) {
			return null;
		}
		for (i in 0...count) {
			var info = mem.readPointer(array.offset(align.ptr * i));
			if (!info.isNull() && mem.readI32(info) == threadId) {
				return info;
			}
		}
		return null;
	}

	// Both exception bits live in the flags word's first (little-endian) byte;
	// the throwing thread is frozen while we touch it, so the single-byte
	// read-modify-write cannot race the debuggee.
	function readFlagsByte(info:Pointer):Int {
		var buf = haxe.io.Bytes.alloc(1);
		api.readMemory(pid, info.offset(align.threadFlags), buf, 1);
		return buf.get(0);
	}

	function writeFlagsByte(info:Pointer, value:Int):Void {
		var buf = haxe.io.Bytes.alloc(1);
		buf.set(0, value & 0xFF);
		api.writeMemory(pid, info.offset(align.threadFlags), buf, 1);
	}
}
