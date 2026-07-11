package debug.target;

import debug.Pointer;

import haxe.Int64;
import haxe.io.Bytes;

/**
 * Typed little-endian reads of debuggee memory over DebugApi. All value decoding
 * goes through this, so it is exercised in tests with a fake API over in-memory
 * bytes.
 */
class MemoryReader {
	final api:DebugApi;
	final pid:Int;
	public final pointerSize:Int;

	public function new(api:DebugApi, pid:Int, is64:Bool) {
		this.api = api;
		this.pid = pid;
		this.pointerSize = is64 ? 8 : 4;
	}

	public function read(address:Pointer, size:Int):Bytes {
		var buf = Bytes.alloc(size);
		api.readMemory(pid, address, buf, size);
		return buf;
	}

	public function readU8(address:Pointer):Int {
		return read(address, 1).get(0);
	}

	public function readU16(address:Pointer):Int {
		return read(address, 2).getUInt16(0);
	}

	public function readI32(address:Pointer):Int {
		return read(address, 4).getInt32(0);
	}

	public function readF32(address:Pointer):Float {
		return read(address, 4).getFloat(0);
	}

	public function readF64(address:Pointer):Float {
		return read(address, 8).getDouble(0);
	}

	public function readI64(address:Pointer):Int64 {
		var b = read(address, 8);
		return Int64.make(b.getInt32(4), b.getInt32(0));
	}

	public function readPointer(address:Pointer):Pointer {
		if (pointerSize == 4) {
			return Int64.make(0, read(address, 4).getInt32(0));
		}
		var b = read(address, 8);
		return Int64.make(b.getInt32(4), b.getInt32(0));
	}
}
