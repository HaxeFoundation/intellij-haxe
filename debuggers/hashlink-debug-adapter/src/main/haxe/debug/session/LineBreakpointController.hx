package debug.session;

import debug.DebugError;
import debug.breakpoints.BreakpointPlanner;
import debug.breakpoints.BreakpointResult;
import debug.breakpoints.PatchedBreakpoint;
import debug.breakpoints.RequestedBreakpoint;

/**
	Source line breakpoints: resolving and installing setBreakpoints requests,
	keeping the currently-stopped-on breakpoint reconciled across re-installs,
	handling user-breakpoint trap hits and evaluating breakpoint conditions.

	A friend of DebugSession (@:access): it drives the session's trap machinery
	(pause-for-memory-write, enterStopped, resumePastSuppressedTrap); the
	session routes setBreakpoints commands and user-breakpoint trap hits here.
**/
@:access(debug.session.DebugSession)
class LineBreakpointController {
	final session:DebugSession;

	public function new(session:DebugSession) {
		this.session = session;
	}

	public function handleSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, requested:Array<RequestedBreakpoint>, isReverify:Bool):Void {
		if (session.module == null) {
			// not launched yet; cannot resolve. Should not happen (dispatcher buffers), but stay safe.
			var pending = [for (r in requested) BreakpointPlanner.unresolved(r, sourcePath)];
			emitBreakpointResults(requestSeq, pending, isReverify);
			return;
		}

		var planned = BreakpointPlanner.plan(session.module, session.jit, sourcePath, requested);

		// while the debuggee runs we must stop it before writing its memory
		var wasRunning = switch (session.state) { case Running: true; default: false; };
		if (wasRunning) {
			session.pauseForMemoryWrite();
		}
		session.breakpoints.setForSource(sourceKey, planned.locations);
		// setForSource re-armed this source's breakpoints; if we are stopped on one of
		// them it just re-planted its INT3 at the current instruction pointer. Lift that
		// INT3 again (keep it suspended) and re-point currentStoppedBreakpoint to the
		// re-installed instance, so the next continue single-steps the real instruction
		// instead of stepping straight into the fresh INT3 and re-hitting the same line
		// (run-to-cursor, or toggling a breakpoint in this file while stopped).
		reconcileStoppedBreakpoint();
		if (wasRunning) {
			session.resumeAfterMemoryWrite();
		}

		emitBreakpointResults(requestSeq, planned.results, isReverify);
	}

	// Keeps the breakpoint we are currently stopped on suspended (INT3 lifted) across a
	// setForSource re-install. No-op when running, or when the stopped breakpoint is not
	// an address-keyed line breakpoint (e.g. an exception breakpoint) or was removed.
	function reconcileStoppedBreakpoint():Void {
		if (session.currentStoppedBreakpoint == null) {
			return;
		}
		var reinstalled = session.breakpoints.atAddress(session.currentStoppedBreakpoint.address);
		if (reinstalled != null) {
			session.breakpoints.suspend(reinstalled);
			session.currentStoppedBreakpoint = reinstalled;
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
		// A conditional breakpoint only stops when its expression is true.
		// Evaluate it against the hitting thread's top frame; a false result
		// resumes without stopping (and WITHOUT ending an in-flight step — the
		// step's temps are still planted, so it keeps progressing).
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

	// Evaluates a conditional breakpoint against the hitting thread's top frame.
	// FAIL SAFE: any error (bad expression, non-Bool result, no frame) stops the
	// debuggee and reports the reason, so a broken condition is never silently
	// skipped — the user always sees why.
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
