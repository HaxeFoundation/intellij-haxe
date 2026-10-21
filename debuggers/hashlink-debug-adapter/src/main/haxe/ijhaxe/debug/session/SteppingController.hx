package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.layout.FrameLayout;
import ijhaxe.debug.module.CodeGraph;
import ijhaxe.debug.target.WaitOutcome;

import haxe.Int64;

/**
	Source-level stepping: next, stepIn, stepOut and smart step into. A step's
	landings are the code addresses where it may stop. This class computes them
	from the control-flow graph, plants temporary INT3s ("temps") there, and
	decides whether a temp hit ends the step.

	A friend of DebugSession (see there): it uses the session's trap machinery
	(stepOverAndResume, stepPastTempAndResume, enterStopped) and owns the
	transitions of the session's `activeStep`.
**/
@:access(ijhaxe.debug.session.DebugSession)
class SteppingController {
	final session:DebugSession;
	// Maps registers to frame slots, to read a closure operand at a stop (the
	// layout the locals view uses). Created lazily: jit exists only after launch.
	var frameLayout:Null<FrameLayout> = null;

	public function new(session:DebugSession) {
		this.session = session;
	}

	function layout():FrameLayout {
		if (frameLayout == null) {
			frameLayout = new FrameLayout(new Align(session.jit.is64, session.jit.boolSize4), session.jit.winCall);
		}
		return frameLayout;
	}

	public function handleStep(requestSeq:Int, threadId:Int, mode:StepMode, targetId:Null<Int>):Void {
		switch (session.state) {
			case Stopped(_):
				var interrupted = plantStepAndResume(threadId, mode, targetId);
				session.state = Running;
				session.emit(EvStepStarted(requestSeq)); // ack now; the stopped(reason:"step") event follows
				if (interrupted != null) {
					// another thread stopped the debuggee during the trap dance:
					// report that stop right after the step response
					session.handleWaitOutcome(interrupted);
				} else if (session.activeStep == null) {
					// No landing could be planted (the only "next" is a native return
					// that cannot be resolved). Behave like continue and report the
					// debuggee as running, so the client does not wait for a step
					// stop that cannot come. A step with planted landings waits for
					// them however long the code runs.
					session.emit(EvResumed(threadId));
				}
			default:
				session.reject(requestSeq, "Cannot step: debuggee is not stopped");
		}
	}

	public function handleStepInTargets(requestSeq:Int, frameId:Int):Void {
		switch (session.state) {
			case Stopped(threadId):
				session.emit(EvStepInTargets(requestSeq, computeStepInTargets(frameId, threadId)));
			default:
				session.reject(requestSeq, "Cannot list step-in targets: debuggee is not stopped");
		}
	}

	// The calls on the stopped line of `frameId`, offered as smart-step-into
	// choices. Only the newest frame can step, so any other frame gets an empty
	// list rather than an error; the client asks according to its UI state. A
	// closure call is labeled with the function its runtime value holds (see
	// closureCallEntry). Callees that cannot be resolved (an unassigned register,
	// a native-function closure, a virtual call) are left out.
	function computeStepInTargets(frameId:Int, threadId:Int):Array<StepInTargetInfo> {
		var frame = session.inspector.frameAt(frameId);
		if (frame == null || frame.index != 0) {
			return [];
		}
		var fidx = frame.location.fidx;
		var startOp = frame.location.op;
		var startLine = session.module.lineOf(fidx, startOp);
		var graph = new CodeGraph(session.module.opcodes(fidx));
		var eip = session.api.readRegister(session.debuggeePid, threadId, Eip);

		var targets = graph.stepTargets(startOp, startLine, (op) -> session.module.lineOf(fidx, op),
			callAtOpAlreadyRan(eip, fidx, startOp));
		var callOps = targets.callOps.copy();
		callOps.sort((a, b) -> a - b); // the graph walk is depth-first; list in execution order

		var result:Array<StepInTargetInfo> = [];
		for (op in callOps) {
			var callee = session.module.callTargetFunction(fidx, op);
			if (callee >= 0) {
				result.push({id: op, label: session.module.functionName(callee)});
			} else {
				var entry = closureCallEntry(threadId, fidx, op);
				var position = entry != null ? session.jit.resolveAddress(entry) : null;
				if (position != null) {
					result.push({id: op, label: session.module.functionName(position.fidx)});
				}
			}
		}
		return result;
	}

	// Where the callee of the closure call at `op` starts, or null. A closure
	// call has no static findex: the frame slot of its operand register holds a
	// vclosure pointer, whose `fun` field (@ +ptr) is the callee's jitted entry.
	// Null when the op is not a closure call, when the register holds no closure
	// yet (it is assigned later on the same line), or when the entry lies
	// outside known jitted code (a native-function closure). An INT3 must never
	// land on a guessed address.
	//
	// The result is the callee's op-0 address (addressOf), not the raw `fun`
	// pointer. `fun` is the true entry at the start of the prologue, before the
	// address of op 0. A landing there stops mid-prologue at an address that
	// resolveAddress cannot map, and the next step then finds no bytecode
	// position and degrades to a plain resume. Static calls land at
	// addressOf(callee, 0), and closure calls must match.
	function closureCallEntry(threadId:Int, fidx:Int, op:Int):Null<Pointer> {
		var closureReg = session.module.closureCallRegister(fidx, op);
		if (closureReg < 0) {
			return null;
		}
		var frames = session.stackWalker.walk(threadId);
		if (frames.length == 0) {
			return null; // no newest frame to read the operand from
		}
		var offsets = layout().registerOffsets(session.module.registers(fidx), session.module.argCount(fidx));
		if (closureReg >= offsets.length) {
			return null;
		}
		var closurePtr = session.memReader.readPointer(
			Int64.add(frames[0].ebp, Int64.ofInt(offsets[closureReg].offset)));
		if (closurePtr.isNull()) {
			return null;
		}
		var fun = session.memReader.readPointer(closurePtr.offset(session.jit.is64 ? 8 : 4));
		if (fun.isNull()) {
			return null;
		}
		var position = session.jit.resolveAddress(fun);
		if (position == null) {
			return null; // outside known jitted code (a native-function closure)
		}
		return session.jit.addressOf(position.fidx, 0);
	}

	// Plants temps at this step's landings, then resumes by stepping over the
	// instruction the thread is stopped on. Afterwards `activeStep` is null when
	// no landing could be planted, and the caller downgrades the step to a
	// continue. Returns a pending debug event when another thread interrupted the
	// trap dance.
	//
	// `targetId` (stepIn only) enters only the call at that opcode, a smart step
	// into choice from stepInTargets. The line-change and return landings stay
	// planted as a fallback, so a chosen call that never executes (short circuit,
	// a condition) ends as a step-over stop instead of running away.
	function plantStepAndResume(threadId:Int, mode:StepMode, targetId:Null<Int>):Null<WaitOutcome> {
		session.breakpoints.clearTemps();
		session.activeStep = null;
		var startEsp = session.api.readRegister(session.debuggeePid, threadId, Esp);

		var eip = session.api.readRegister(session.debuggeePid, threadId, Eip);
		var position = session.jit.resolveAddress(eip);
		if (position == null) {
			// not in known bytecode (e.g. inside a native call): can't compute targets
			return session.stepOverAndResume(threadId);
		}
		var fidx = position.fidx;
		var startLine = session.module.lineOf(fidx, position.op);
		var graph = new CodeGraph(session.module.opcodes(fidx));
		var targets = graph.stepTargets(position.op, startLine, (op) -> session.module.lineOf(fidx, op),
			callAtOpAlreadyRan(eip, fidx, position.op));
		var returnAddress = currentReturnAddress(threadId);
		var targetedEntry:Null<Pointer> = null;
		var targetedCallSite:Null<{fidx:Int, op:Int}> = null;
		var pendingClosureSites:Array<{address:Pointer, fidx:Int, op:Int}> = [];

		if (mode == StepOut) {
			// step out: stop only when the current function returns
			if (returnAddress != null) {
				session.breakpoints.addTemp(returnAddress);
			}
		} else {
			for (op in targets.lineChangeOps) {
				session.breakpoints.addTemp(session.jit.addressOf(fidx, op));
			}
			if (targets.returns && returnAddress != null) {
				session.breakpoints.addTemp(returnAddress);
			}
			if (mode == StepIn) {
				for (op in targets.callOps) {
					if (targetId != null && op != targetId) {
						continue; // targeted step: only the chosen call's entry
					}
					var callee = session.module.callTargetFunction(fidx, op);
					// a static callee's entry is its first opcode; a closure call's
					// entry resolves from the closure's RUNTIME value at this stop
					var entry:Null<Pointer> = callee >= 0
						? session.jit.addressOf(callee, 0)
						: closureCallEntry(threadId, fidx, op);
					if (entry != null) {
						session.breakpoints.addTemp(entry);
						if (targetId != null) {
							targetedEntry = entry;
							targetedCallSite = {fidx: fidx, op: op};
						}
					} else if (targetId == null && session.module.closureCallRegister(fidx, op) >= 0) {
						// A closure call whose operand register is not set yet, because
						// the closure is produced earlier on this line (`functions[0]()`).
						// Defer it by trapping the call op itself. When execution gets
						// there the operand is known, the entry resolves, and the hit
						// resumes into the callee; it is never a landing.
						var siteAddress = session.jit.addressOf(fidx, op);
						session.breakpoints.addTemp(siteAddress);
						pendingClosureSites.push({address: siteAddress, fidx: fidx, op: op});
					}
				}
			}
		}

		if (session.breakpoints.hasTemps()) {
			session.activeStep = {
				threadId: threadId, mode: mode, startEsp: startEsp,
				targetEntry: targetedEntry, targetCallSite: targetedCallSite,
				pendingClosureSites: pendingClosureSites.length > 0 ? pendingClosureSites : null
			};
		}
		return session.stepOverAndResume(threadId);
	}

	function currentReturnAddress(threadId:Int):Null<Pointer> {
		var frames = session.stackWalker.walk(threadId);
		return frames.length >= 2 ? frames[1].address : null;
	}

	// True when the thread stopped past the first native byte of `op`: the op's
	// call has already run and the thread sits at its return address. That is the
	// only mid-op stop a user sees, because temps and user breakpoints sit at op
	// starts. Such a call must not be offered or planted as enterable again.
	function callAtOpAlreadyRan(eip:Pointer, fidx:Int, op:Int):Bool {
		return Int64.compare(eip, session.jit.addressOf(fidx, op)) > 0;
	}

	// Handles a hit on a step temp. Temps sit at code addresses, so every thread
	// that executes the line traps on them. A hit by a thread that does not own
	// the step is never its landing; that thread is stepped past and runs on.
	// The owning thread must also pass the recursion frame guard (step over and
	// out).
	public function handleTempHit(threadId:Int, hitAddress:Pointer):Void {
		var step = session.activeStep;
		if (step != null && (threadId != step.threadId || !frameGuardSatisfied(step))) {
			session.stepPastTempAndResume(threadId, hitAddress);
			return;
		}
		if (step != null && resolvePendingClosureSite(step, threadId, hitAddress)) {
			// Not a landing: this temp only reads the closure operand at its call
			// site. The callee's entry temp is planted now, or the callee cannot be
			// resolved and the step falls back to its line and return landings.
			// Either way, run on.
			session.stepPastTempAndResume(threadId, hitAddress);
			return;
		}
		if (step != null && !targetedCallSiteSatisfied(step, threadId, hitAddress)) {
			session.stepPastTempAndResume(threadId, hitAddress);
			return;
		}
		session.enterStopped(threadId, null);
		session.emit(EvStoppedStep(threadId));
	}

	// True when `hitAddress` is one of this step's deferred closure call sites.
	// Everything before the call has executed, so the operand register holds the
	// closure: this resolves the callee entry and plants its temp. A site is used
	// up by its first hit; a loop that re-enters the line gets new sites from the
	// next step.
	function resolvePendingClosureSite(step:ActiveStep, threadId:Int, hitAddress:Pointer):Bool {
		var sites = step.pendingClosureSites;
		if (sites == null) {
			return false;
		}
		for (site in sites) {
			if (Int64.compare(hitAddress, site.address) == 0) {
				var entry = closureCallEntry(threadId, site.fidx, site.op);
				if (entry != null) {
					session.breakpoints.addTemp(entry);
				}
				sites.remove(site);
				return true;
			}
		}
		return false;
	}

	// A targeted step-in plants its entry temp at the callee function, which the
	// line may call more than once (`cfg.test1(1)` ... `test1(2)`). The landing
	// counts only when the new frame's return address points back at the chosen
	// call op. Other landings (line change, return) and untargeted steps always
	// count.
	function targetedCallSiteSatisfied(step:ActiveStep, threadId:Int, hitAddress:Pointer):Bool {
		if (step.targetEntry == null || Int64.compare(hitAddress, step.targetEntry) != 0) {
			return true;
		}
		var site = step.targetCallSite;
		if (site == null) {
			return true;
		}
		var frames = session.stackWalker.walk(threadId);
		if (frames.length < 2) {
			return false; // no caller frame: cannot be the chosen call site
		}
		// Resolve the byte before the return address: it always lies inside the
		// call's op, while the return address itself can fall on the next op's
		// start (when the op emits nothing after the call).
		var caller = session.jit.resolveAddress(Int64.sub(frames[1].address, Int64.ofInt(1)));
		return caller != null && caller.fidx == site.fidx && caller.op == site.op;
	}

	// The recursion frame guard. The stack grows down, so a frame at the step's
	// depth or shallower has esp >= the esp at step start. The comparison only
	// means something on the step's own thread, since each thread has its own
	// stack; hits by other threads are filtered out before this runs.
	function frameGuardSatisfied(step:ActiveStep):Bool {
		if (step.mode == StepIn) {
			return true; // any landing (same-frame line change or callee entry) is valid
		}
		var esp = session.api.readRegister(session.debuggeePid, step.threadId, Esp);
		return Int64.compare(esp, step.startEsp) >= 0;
	}
}
