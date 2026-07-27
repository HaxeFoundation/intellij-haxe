package ijhaxe.debug.target;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.Pointer;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Typed little-endian writes of debuggee memory over DebugApi — the value
	modification counterpart of MemoryReader. Only used while the debuggee is
	stopped; a failed write throws (unlike reads, a silent partial write would
	corrupt the debuggee).
**/
class MemoryWriter {
	final api:DebugApi;
	final pid:Int;
	public final pointerSize:Int;

	public function new(api:DebugApi, pid:Int, is64:Bool) {
		this.api = api;
		this.pid = pid;
		this.pointerSize = is64 ? 8 : 4;
	}

	public function write(address:Pointer, bytes:Bytes):Void {
		if (!api.writeMemory(pid, address, bytes, bytes.length)) {
			throw new DebugError("Cannot write " + bytes.length + " bytes to the debuggee");
		}
	}

	public function writeU8(address:Pointer, value:Int):Void {
		var b = Bytes.alloc(1);
		b.set(0, value & 0xFF);
		write(address, b);
	}

	public function writeU16(address:Pointer, value:Int):Void {
		var b = Bytes.alloc(2);
		b.setUInt16(0, value & 0xFFFF);
		write(address, b);
	}

	public function writeI32(address:Pointer, value:Int):Void {
		var b = Bytes.alloc(4);
		b.setInt32(0, value);
		write(address, b);
	}

	public function writeF32(address:Pointer, value:Float):Void {
		var b = Bytes.alloc(4);
		b.setFloat(0, value);
		write(address, b);
	}

	public function writeF64(address:Pointer, value:Float):Void {
		var b = Bytes.alloc(8);
		b.setDouble(0, value);
		write(address, b);
	}

	public function writeI64(address:Pointer, value:Int64):Void {
		var b = Bytes.alloc(8);
		b.setInt32(0, value.low);
		b.setInt32(4, value.high);
		write(address, b);
	}

	public function writePointer(address:Pointer, value:Pointer):Void {
		if (pointerSize == 4) {
			writeI32(address, value.low);
			return;
		}
		writeI64(address, value);
	}
}
