package debug;

import haxe.io.Bytes;

/**
 * The OS-level process-control operations the debugger needs, abstracted over
 * the HashLink `debug_*` natives.
 *
 * This interface exists for two reasons: the real implementation
 * (HlNativeDebugApi) only compiles on the HL target, and tests can substitute
 * a fake to exercise the session/breakpoint logic under the interpreter.
 *
 * IMPORTANT: on Windows every call must run on the one thread that called
 * `start` (DebugActiveProcess / WaitForDebugEvent affinity). DebugSession owns
 * that thread; nothing else may call these methods.
 */
interface DebugApi {
	/** Attach to the debuggee process (ptrace / DebugActiveProcess / task_for_pid). */
	function start(pid:Int):Bool;

	/** Detach from the debuggee. */
	function stop(pid:Int):Void;

	/** Force the running debuggee to stop as soon as possible. */
	function forceBreak(pid:Int):Bool;

	/** Read `size` bytes of debuggee memory at `addr` into `buffer`. */
	function readMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool;

	/** Write `size` bytes from `buffer` into debuggee memory at `addr`. */
	function writeMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool;

	/** Flush the instruction cache for a patched code range (needed after writes on some OSes). */
	function flush(pid:Int, addr:Pointer, size:Int):Bool;

	/** Block up to `timeoutMs` for a debug event. */
	function wait(pid:Int, timeoutMs:Int):WaitOutcome;

	/** Resume a stopped thread. */
	function resume(pid:Int, threadId:Int):Bool;

	/** Read a CPU register's value as an address. */
	function readRegister(pid:Int, threadId:Int, register:Register):Pointer;

	/** Write a CPU register. */
	function writeRegister(pid:Int, threadId:Int, register:Register, value:Pointer):Bool;
}
