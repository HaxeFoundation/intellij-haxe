package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;

/**
	The in-flight step, bound to the thread that started it. All threads stop
	together, a step starts at a stop and ends at the next one, so at most one
	step exists at a time and the session needs no per-thread map.

	The thread binding matters because step temps sit at code addresses, and
	every thread that executes the line traps on them. A temp hit by a thread
	other than `threadId` is never this step's landing; that thread is stepped
	past and resumed. Its stack pointer also belongs to a different stack, so
	comparing it with `startEsp` would mean nothing.
**/
typedef ActiveStep = {
	var threadId:Int;
	var mode:StepMode;

	// the stepping thread's stack pointer at step start (recursion frame guard)
	var startEsp:Pointer;

	// Targeted step-in only (smart step into a chosen call): the callee's entry
	// address and the chosen call op. The entry temp sits at the function, which
	// the line may call more than once (`cfg.test1(1)` ... `test1(2)`). A hit
	// ends the step only when the new frame's return address points back at
	// the chosen op; other calls are stepped past.
	var ?targetEntry:Pointer;
	var ?targetCallSite:{fidx:Int, op:Int};

	// Step-in only: closure call sites whose operand register was not yet set
	// at the stop, because the closure is produced earlier on the same line
	// (`functions[0]()`). A temp sits at each site's op start. Hitting it is
	// never a landing: the operand is known there, so the callee entry
	// resolves, gets its temp, and execution runs on into it.
	var ?pendingClosureSites:Array<{address:Pointer, fidx:Int, op:Int}>;
}
