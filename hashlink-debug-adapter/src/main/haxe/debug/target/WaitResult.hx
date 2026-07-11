package debug.target;
import dap.protocol.Breakpoint;

/**
 * Classification of a debug event returned by DebugApi.wait, mirroring the
 * integer codes of the HashLink `debug_wait` native (src/std/debug.c):
 * -1 timeout, 0 exit, 1 breakpoint, 2 single-step, 3 error, 4 handled,
 * 5 stack overflow.
 */
enum abstract WaitResult(Int) from Int to Int {
	var Timeout = -1;
	var Exit = 0;
	var Breakpoint = 1;
	var SingleStep = 2;
	var Error = 3;
	var Handled = 4;
	var StackOverflow = 5;
}
