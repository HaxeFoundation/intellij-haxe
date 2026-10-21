package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.breakpoints.PatchedBreakpoint;
import ijhaxe.debug.target.StackFrameLocation;

/**
	Exception breakpoints: the "all", "uncaught", "vm" and per-type filters. It
	arms and disarms the INT3s at OThrow sites and at hl_throw's entry, and
	handles hits on both.

	A VM-raised throw stops in two phases. The thrown value cannot be read at
	hl_throw's entry, so the throwing frames are walked and parked there, and the
	stop is reported later at hl_throw's own break.

	A friend of DebugSession (see there): it uses the session's trap machinery
	(pauseForMemoryWrite, enterStopped, resumePastSuppressedTrap).
**/
@:access(ijhaxe.debug.session.DebugSession)
class ExceptionController {
	final session:DebugSession;

	// "all" breaks on every throw; "uncaught" only when no live `try` catches it.
	var breakAll:Bool = false;
	var breakUncaught:Bool = false;

	// the per-type filter: full or simple class names of the exceptions to stop on
	var breakTypes:Array<String> = [];

	// The "vm" filter: break on VM-raised errors (null access, out of bounds,
	// invalid cast) by trapping hl_throw. Its address is mined once from an
	// OThrow site and cached.
	var breakVm:Bool = false;
	var nativeThrowAddress:Null<Pointer> = null;

	// Threads between hl_throw's entry trap (where the thrown value cannot be
	// read) and hl_throw's own hl_debug_break (where exc_value holds it), with
	// the throwing frames walked at the entry; they cannot be walked at the break.
	final pendingVmThrow:Map<Int, Bool> = new Map();
	final vmThrowFrames:Map<Int, Array<StackFrameLocation>> = new Map();

	public function new(session:DebugSession) {
		this.session = session;
	}

	public function setFilters(requestSeq:Int, filters:Array<String>, filterTypes:Array<String>):Void {
		breakAll = filters != null && filters.indexOf("all") >= 0;
		breakUncaught = filters != null && filters.indexOf("uncaught") >= 0;
		breakVm = filters != null && filters.indexOf("vm") >= 0;
		breakTypes = filterTypes != null ? filterTypes : [];
		apply();
		session.emit(EvExceptionBreakpointsSet(requestSeq));
	}

	/**
		Makes the armed exception INT3s match the filters. There are two
		independent traps. The OThrow sites are armed while "all", "uncaught" or a
		type filter is on; the filters only decide whether a hit stops. hl_throw's
		entry is armed for the "vm" filter and catches VM-raised errors that
		execute no bytecode throw. Does nothing before launch, when breakpoints and
		sites do not exist yet; the launch runs it again. Arming writes debuggee
		memory, so a running debuggee is paused briefly.
	**/
	public function apply():Void {
		if (session.breakpoints == null || session.exceptionSites == null) {
			return;
		}
		var wantSites = breakAll || breakUncaught || breakTypes.length > 0;
		var wantVm = breakVm && resolveNativeThrow() != null;
		var sitesChange = wantSites != session.breakpoints.isExceptionsArmed();
		var vmChange = wantVm != session.breakpoints.isNativeThrowArmed();
		if (!sitesChange && !vmChange) {
			return;
		}
		var wasRunning = switch (session.state) { case Running: true; default: false; };
		var pausedForWrite = wasRunning && session.pauseForMemoryWrite();
		if (sitesChange) {
			if (wantSites) session.breakpoints.armExceptions(session.exceptionSites.all());
			else session.breakpoints.disarmExceptions();
		}
		if (vmChange) {
			if (wantVm) {
				session.breakpoints.armNativeThrow(nativeThrowAddress);
			} else {
				session.breakpoints.disarmNativeThrow();
				// drop throws parked between the entry trap and hl_throw's own
				// break: the filter is off, so they must not surface as stops
				for (threadId in pendingVmThrow.keys()) {
					session.vmExceptions.disarmCatchAll(threadId);
				}
				pendingVmThrow.clear();
				vmThrowFrames.clear();
			}
		}
		if (pausedForWrite) {
			session.resumeAfterMemoryWrite();
		}
	}

	// hl_throw's address, mined once from an OThrow site and cached. Null when
	// the program has no throw site or the machine-code pattern is not
	// recognized; the "vm" filter then cannot arm.
	function resolveNativeThrow():Null<Pointer> {
		if (nativeThrowAddress == null && session.nativeThrowResolver != null) {
			nativeThrowAddress = session.nativeThrowResolver.resolve();
			if (nativeThrowAddress == null) {
				session.emit(EvOutput("console",
					"HashLink VM exceptions breakpoint unavailable: could not locate hl_throw "
					+ "(the program has no throw site to mine, or the JIT pattern was not recognised).\n"));
			}
		}
		return nativeThrowAddress;
	}

	// A throw site was hit while exception breakpoints are armed. "all" stops on
	// every throw, "uncaught" only when no live `try` catches it, and a type
	// filter on a matching class. A throw that does not stop is stepped past
	// silently, like a false breakpoint condition, so its catch runs.
	public function handleSiteHit(threadId:Int, throwSite:{bp:PatchedBreakpoint, reg:Int}):Void {
		var stop = breakAll
			|| (breakUncaught && session.throwClassifier.isUncaught(threadId))
			|| session.throwClassifier.throwMatchesTypes(threadId, throwSite.reg, breakTypes);
		if (!stop) {
			session.resumePastSuppressedTrap(threadId, throwSite.bp);
			return;
		}
		// Restore the original byte so the throw runs on continue: the trap dance
		// in stepOverAndResume single-steps it and re-arms the site. The stop is
		// reported before the throw executes, so the top frame is the thrower.
		session.breakpoints.suspend(throwSite.bp);
		session.enterStopped(threadId, throwSite.bp);
		session.emit(EvStoppedException(threadId, session.descriptions.thrown(threadId, throwSite.reg)));
	}

	// hl_throw's entry, which every exception passes. Only VM-raised errors
	// (null access, out of bounds, invalid cast) surface here: throws whose
	// immediate caller is C runtime code. A bytecode throw's caller is jitted
	// code; it is resumed silently here and left to the OThrow-site
	// breakpoints, which avoids a double stop.
	//
	// The thrown value cannot be read at this entry: it sits in an argument
	// register that HL's debug API does not expose. So a VM-raised throw does
	// not stop here either. HL_EXC_CATCH_ALL is set on the throwing thread and
	// hl_throw runs on. It stores exc_value and then executes its own
	// hl_debug_break, where handleVmThrowBreak reports the stop with the actual
	// error message. Only when the thread registry cannot be read does the stop
	// happen here, with a generic description.
	public function handleNativeThrowHit(threadId:Int):Void {
		var syntheticBp = session.breakpoints.nativeThrowBreakpoint();
		var vmRaised = session.throwClassifier.raisedByRuntime(threadId);
		if (vmRaised && !(session.vmExceptions != null && session.vmExceptions.armCatchAll(threadId))) {
			session.breakpoints.suspend(syntheticBp);
			session.enterStopped(threadId, syntheticBp);
			session.emit(EvStoppedException(threadId, session.descriptions.vmThrow(threadId, null)));
			return;
		}
		if (vmRaised) {
			pendingVmThrow.set(threadId, true);
			// walked here: at hl_throw's own break the frame chain is gone
			vmThrowFrames.set(threadId, session.stackWalker.walk(threadId));
		}
		session.resumePastSuppressedTrap(threadId, syntheticBp);
	}

	/**
		Handles hl_throw's own hl_debug_break, requested at the entry trap.
		exc_value now holds the thrown vdynamic. EIP is already past the VM's
		int3, so a later continue resumes plainly, without a trap dance. True
		when the trap belongs to a parked VM throw and the stop was reported;
		false for any other trap (attach or loader noise).
	**/
	public function handleVmThrowBreak(threadId:Int):Bool {
		if (!pendingVmThrow.exists(threadId) || session.vmExceptions == null || !session.vmExceptions.isThrowBreak(threadId)) {
			return false;
		}
		pendingVmThrow.remove(threadId);
		session.vmExceptions.disarmCatchAll(threadId);
		session.enterStopped(threadId, null);
		session.emit(EvStoppedException(threadId, session.descriptions.vmThrow(threadId, session.vmExceptions.thrownValue(threadId))));
		return true;
	}

	/**
		Takes the frames walked at hl_throw's entry, for the stop reported at
		hl_throw's own break, where the frame chain can no longer be walked. Null
		when nothing is parked.
	**/
	public function consumeParkedFrames(threadId:Int):Null<Array<StackFrameLocation>> {
		var parked = vmThrowFrames.get(threadId);
		if (parked != null) {
			vmThrowFrames.remove(threadId);
		}
		return parked;
	}
}
