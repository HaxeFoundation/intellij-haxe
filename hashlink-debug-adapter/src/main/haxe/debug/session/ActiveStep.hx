package debug.session;

import debug.Pointer;

/**
 * The one in-flight step, bound to the thread that initiated it. Suspend-all
 * makes this a singleton rather than a per-thread map: a step can only start
 * from a stop and ends at the next stop, so at most one exists at a time.
 *
 * The binding matters because temporary breakpoints live at CODE addresses:
 * any thread executing that line traps on them. A temp hit by a thread other
 * than `threadId` is never this step's landing (and its `startEsp` belongs to
 * a different stack, so the recursion frame-guard comparison would be
 * meaningless for it) — such hits are single-stepped past and resumed.
 */
typedef ActiveStep = {
	var threadId:Int;
	var mode:StepMode;
	// the stepping thread's stack pointer at step start (recursion frame guard)
	var startEsp:Pointer;
}
