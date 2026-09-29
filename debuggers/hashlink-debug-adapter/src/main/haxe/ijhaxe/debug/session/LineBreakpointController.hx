package ijhaxe.debug.session;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.breakpoints.BreakpointPlanner;
import ijhaxe.debug.breakpoints.BreakpointResult;
import ijhaxe.debug.breakpoints.PatchedBreakpoint;
import ijhaxe.debug.breakpoints.RequestedBreakpoint;

/**
	Source line breakpoints: resolves and installs setBreakpoints requests, keeps
	the breakpoint a stop is on consistent across re-installs, and handles hits
	on user breakpoints, including their conditions.

	A friend of DebugSession (see there): it uses the session's trap machinery
	(pauseForMemoryWrite, enterStopped, resumePastSuppressedTrap).
**/
@:access(ijhaxe.debug.session.DebugSession)
class LineBreakpointController {
	final session:DebugSession;

	public function new(session:DebugSession) {
		this.session = session;
	}

	public function handleSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, requested:Array<RequestedBreakpoint>, isReverify:Bool):Void {
		if (session.module == null) {
			// not launched, so nothing can be resolved; the dispatcher holds requests until launch
			var pending = [for (r in requested) BreakpointPlanner.unresolved(r, sourcePath)];
			emitBreakpointResults(requestSeq, pending, isReverify);
			return;
		}

		var planned = BreakpointPlanner.plan(session.module, session.jit, sourcePath, requested);

		// a running debuggee has to be stopped before its memory can be written
		var wasRunning = switch (session.state) { case Running: true; default: false; };
		var pausedForWrite = wasRunning && session.pauseForMemoryWrite();
		session.breakpoints.setForSource(sourceKey, planned.locations);
		// setForSource re-armed this source's breakpoints. If the stop is on one of
		// them, its INT3 is back at the current instruction pointer. Lift it again
		// and point currentStoppedBreakpoint at the re-installed instance. Otherwise
		// the next continue runs into the fresh INT3 and hits the same line again
		// (run to cursor, or toggling a breakpoint in this file while stopped).
		reconcileStoppedBreakpoint();
		if (pausedForWrite) {
			session.resumeAfterMemoryWrite();
		}

		emitBreakpointResults(requestSeq, planned.results, isReverify);
	}

	// Keeps the breakpoint the session is stopped on suspended (INT3 lifted)
	// across a setForSource re-install. Does nothing while running, or when the
	// stop is not on a line breakpoint (an exception breakpoint, for example).
	function reconcileStoppedBreakpoint():Void {
		if (session.currentStoppedBreakpoint == null) {
			return;
		}
		var reinstalled = session.breakpoints.atAddress(session.currentStoppedBreakpoint.address);
		if (reinstalled != null) {
			session.breakpoints.suspend(reinstalled);
			session.currentStoppedBreakpoint = reinstalled;
		} else {
			// The breakpoint of the stop was removed. Drop the stale reference, or
			// the next continue re-arms the deleted breakpoint's INT3. That orphan
			// trap has no table entry, so its hit is resumed silently with Eip
			// already past the 0xCC. The VM then runs the original instruction
			// without its first byte and faults.
			session.currentStoppedBreakpoint = null;
		}
	}

	function emitBreakpointResults(requestSeq:Int, results:Array<BreakpointResult>, isReverify:Bool):Void {
		if (isReverify) {
			for (result in results) {
				session.emit(EvBreakpointChanged(result));
			}
		} else {
			session.emit(EvBreakpoints(requestSeq, results));
		}
	}

	public function handleHit(threadId:Int, userBp:PatchedBreakpoint):Void {
		// A conditional breakpoint stops only when its condition is true. A false
		// result resumes without stopping and without ending an in-flight step,
		// whose temps stay planted.
		if (userBp.condition != null && userBp.condition != "") {
			session.inspector.startStop(threadId);
			session.stoppedThreadId = threadId; // the condition's eval-calls target this thread
			if (!breakpointConditionHolds(threadId, userBp)) {
				session.resumePastSuppressedTrap(threadId, userBp);
				return;
			}
		}
		session.breakpoints.suspend(userBp);
		session.enterStopped(threadId, userBp);
		session.emit(EvStoppedBreakpoint(threadId, [userBp.id]));
	}

	// Evaluates a breakpoint condition against the hitting thread's top frame.
	// It fails safe: any error (a bad expression, a non-Bool result, no frame)
	// stops the debuggee and prints the reason, so a broken condition is never
	// skipped silently.
	function breakpointConditionHolds(threadId:Int, bp:PatchedBreakpoint):Bool {
		var frames = session.inspector.framesFor(threadId);
		if (frames.length == 0) {
			emitConditionNote(bp, "no stack frame to evaluate against");
			return true;
		}
		try {
			return session.inspector.evaluateBool(frames[0].frameId, bp.condition);
		} catch (e:DebugError) {
			emitConditionNote(bp, e.message);
			return true;
		} catch (e:Dynamic) {
			emitConditionNote(bp, Std.string(e));
			return true;
		}
	}

	function emitConditionNote(bp:PatchedBreakpoint, reason:String):Void {
		session.emit(EvOutput("console", '[debugger] breakpoint condition "${bp.condition}" could not be evaluated ($reason); stopping.'
			+ String.fromCharCode(10)));
	}
}
