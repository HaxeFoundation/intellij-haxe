package debug.session;

import debug.Pointer;

/**
	The one in-flight step, bound to the thread that initiated it. Suspend-all
	makes this a singleton rather than a per-thread map: a step can only start
	from a stop and ends at the next stop, so at most one exists at a time.

	The binding matters because temporary breakpoints live at CODE addresses:
	any thread executing that line traps on them. A temp hit by a thread other
	than `threadId` is never this step's landing (and its `startEsp` belongs to
	a different stack, so the recursion frame-guard comparison would be
	meaningless for it) — such hits are single-stepped past and resumed.
**/
typedef ActiveStep = {
	var threadId:Int;
	var mode:StepMode;
	// the stepping thread's stack pointer at step start (recursion frame guard)
	var startEsp:Pointer;
	// TARGETED step-in only (smart step into a chosen call): the callee's entry
	// address and the chosen call op. The entry temp is at the FUNCTION, which
	// the line may invoke more than once (cfg.test1(1)...test1(2)) — a hit only
	// lands the step when the new frame's return address points back at the
	// chosen op; other invocations are stepped past.
	var ?targetEntry:Pointer;
	var ?targetCallSite:{fidx:Int, op:Int};
	// Step-in only: closure call sites whose operand register was NOT yet
	// populated at the stop (the closure is produced earlier on the same line,
	// e.g. `functions[0]()`). Each site's op start carries a temp; hitting it
	// is never a landing — the operand is in hand THERE, so the callee entry
	// resolves and gets its temp, and execution runs on into it.
	var ?pendingClosureSites:Array<{address:Pointer, fidx:Int, op:Int}>;
}
