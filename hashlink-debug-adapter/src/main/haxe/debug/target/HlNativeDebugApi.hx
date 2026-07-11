package debug.target;

import debug.Pointer;

#if hl
import haxe.Int64;
import haxe.io.Bytes;

/**
 * DebugApi backed by the HashLink VM's `debug_*` natives (library "std",
 * implemented per-OS in hashlink `src/std/debug.c`). Compiled only on the HL
 * target, since @:hlNative bindings do not exist under the interpreter or on
 * other targets.
 *
 * Addresses cross the interface as Int64 (Pointer); here they are bridged to
 * the native `hl.Bytes` pointer type via hl.Bytes.fromAddress / Bytes.address.
 * The register read/write natives take/return the register value as a
 * pointer-typed value, so the same bridge applies.
 */
class HlNativeDebugApi implements DebugApi {
	public function new() {}

	public function start(pid:Int):Bool {
		return debug_start(pid);
	}

	public function stop(pid:Int):Void {
		debug_stop(pid);
	}

	public function forceBreak(pid:Int):Bool {
		return debug_breakpoint(pid);
	}

	public function readMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool {
		return debug_read(pid, hl.Bytes.fromAddress(addr), buffer.getData(), size);
	}

	public function writeMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool {
		return debug_write(pid, hl.Bytes.fromAddress(addr), buffer.getData(), size);
	}

	public function flush(pid:Int, addr:Pointer, size:Int):Bool {
		return debug_flush(pid, hl.Bytes.fromAddress(addr), size);
	}

	public function wait(pid:Int, timeoutMs:Int):WaitOutcome {
		var threadId = 0;
		var code = debug_wait(pid, threadId, timeoutMs);
		return {result: (code : WaitResult), threadId: threadId};
	}

	public function resume(pid:Int, threadId:Int):Bool {
		return debug_resume(pid, threadId);
	}

	public function readRegister(pid:Int, threadId:Int, register:Register):Pointer {
		var value = debug_read_register(pid, threadId, register, true);
		return value.address();
	}

	public function writeRegister(pid:Int, threadId:Int, register:Register, value:Pointer):Bool {
		return debug_write_register(pid, threadId, register, hl.Bytes.fromAddress(value), true);
	}

	// The @:hlNative bodies below are placeholders; the linker replaces them
	// with the "std" library natives on the HL target.
	@:hlNative("std", "debug_start") static function debug_start(pid:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_stop") static function debug_stop(pid:Int):Void {}

	@:hlNative("std", "debug_breakpoint") static function debug_breakpoint(pid:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_read") static function debug_read(pid:Int, addr:hl.Bytes, buffer:hl.Bytes, size:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_write") static function debug_write(pid:Int, addr:hl.Bytes, buffer:hl.Bytes, size:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_flush") static function debug_flush(pid:Int, addr:hl.Bytes, size:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_wait") static function debug_wait(pid:Int, threadId:hl.Ref<Int>, timeout:Int):Int {
		return -1;
	}

	@:hlNative("std", "debug_resume") static function debug_resume(pid:Int, threadId:Int):Bool {
		return false;
	}

	@:hlNative("std", "debug_read_register") static function debug_read_register(pid:Int, threadId:Int, register:Int, is64:Bool):hl.Bytes {
		return null;
	}

	@:hlNative("std", "debug_write_register") static function debug_write_register(pid:Int, threadId:Int, register:Int, value:hl.Bytes, is64:Bool):Bool {
		return false;
	}
}
#end
