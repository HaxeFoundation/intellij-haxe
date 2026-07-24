package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.breakpoints.PatchedBreakpoint;
import ijhaxe.debug.target.StackFrameLocation;

/**
	Exception breakpoints: the "all"/"uncaught"/"vm"/per-type filters, arming
	and disarming the OThrow-site INT3s and the hl_throw entry trap, and the
	trap-hit handling for both — including the two-phase VM-throw dance (the
	thrown value is unreadable at hl_throw's entry, so the stop is reported at
	hl_throw's own break, with the frames parked at the entry).

	A friend of DebugSession (@:access): it drives the session's trap machinery
	(pause-for-memory-write, enterStopped, resumePastSuppressedTrap); the
	session routes filter commands and exception trap hits here.
**/
@:access(ijhaxe.debug.session.DebugSession)
class ExceptionController {
	final session:DebugSession;

	// Which exception modes are active: "all" breaks on every throw; "uncaught"
	// only when no live `try` will catch the throw; `types` on matching classes.
	var breakAll:Bool = false;
	var breakUncaught:Bool = false;
	// FQNs (or simple names) of exception classes to stop on — the per-type filter.
	var breakTypes:Array<String> = [];
	// "vm" filter: break on VM-raised errors (null access, bounds, cast, ...)
	// by trapping hl_throw. Resolved lazily from an OThrow site once, then cached.
	var breakVm:Bool = false;
	var nativeThrowAddress:Null<Pointer> = null;
	// Threads parked between hl_throw's ENTRY trap (where the thrown value is
	// unreadable) and hl_throw's own hl_debug_break (where exc_value holds it),
	// with the throwing frames walked at the entry (unwalkable at the break).
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
		Reconciles the armed exception INT3s with the desired state. Two independent
		traps: OThrow sites (armed while "all"/"uncaught"/types is on — the mode only
		changes whether a hit surfaces) and hl_throw's entry (the "vm" filter,
		catching VM-raised errors with no bytecode throw). A no-op before launch
		(breakpoints/sites not built yet — re-run once they are). Arming/disarming
		writes debuggee memory, so a running debuggee is briefly frozen first.
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
		if (wasRunning) {
			session.pauseForMemoryWrite();
		}
		if (sitesChange) {
			if (wantSites) session.breakpoints.armExceptions(session.exceptionSites.all());
			else session.breakpoints.disarmExceptions();
		}
		if (vmChange) {
			if (wantVm) {
				session.breakpoints.armNativeThrow(nativeThrowAddress);
			} else {
				session.breakpoints.disarmNativeThrow();
				// forget throws parked between the entry trap and hl_throw's own
				// break — the filter is off, so they must not surface as stops
				for (threadId in pendingVmThrow.keys()) {
					session.vmExceptions.disarmCatchAll(threadId);
				}
				pendingVmThrow.clear();
				vmThrowFrames.clear();
			}
		}
		if (wasRunning) {
			session.resumeAfterMemoryWrite();
		}
	}

	// Resolves hl_throw's address once (mined from an OThrow site) and caches it;
	// null when the program has no throw site to mine or the pattern is absent
	// (the VM-exceptions breakpoint then simply cannot arm).
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

	// An exception is being thrown here and the exception breakpoint is armed.
	// "all" stops on every throw; "uncaught" stops only when no live `try` will
	// catch it — a caught throw under uncaught-only is resumed past silently
	// (same trap-dance as a false conditional breakpoint), so the catch runs.
	public function handleSiteHit(threadId:Int, excEntry:{bp:PatchedBreakpoint, reg:Int}):Void {
		var stop = breakAll
			|| (breakUncaught && session.throwClassifier.isUncaught(threadId))
			|| session.throwClassifier.throwMatchesTypes(threadId, excEntry.reg, breakTypes);
		if (!stop) {
			session.resumePastSuppressedTrap(threadId, excEntry.bp);
			return;
		}
		// Restore the original byte so the throw itself runs on continue; the
		// trap dance (stepOverAndResume) single-steps it and re-arms the site.
		// Reported BEFORE the throw executes, so the frame is the throwing function.
		session.breakpoints.suspend(excEntry.bp);
		session.enterStopped(threadId, excEntry.bp);
		session.emit(EvStoppedException(threadId, session.descriptions.thrown(threadId, excEntry.reg)));
	}

	// hl_throw's entry: EVERY exception passes through here. We only surface
	// VM-RAISED errors (null access, bounds, cast, ...) — i.e. throws whose
	// immediate caller is C runtime code, not a jitted OThrow. A bytecode
	// throw's caller IS jit code, so it is left to the OThrow-based breakpoints
	// (avoiding a double stop) and resumed past silently here.
	//
	// The thrown value is UNREADABLE at this entry (it sits in an argument
	// register HL's debug API does not expose), so a VM-raised throw does not
	// stop here either: we set HL_EXC_CATCH_ALL on the throwing thread and let
	// hl_throw run on — it stores exc_value and then executes its own
	// hl_debug_break, where handleVmThrowBreak reports the stop WITH the
	// actual error message. Only when the thread registry is unreadable do we
	// stop here, with a generic description.
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
			// walked HERE: at hl_throw's own break the chain is gone
			vmThrowFrames.set(threadId, session.stackWalker.walk(threadId));
		}
		session.resumePastSuppressedTrap(threadId, syntheticBp);
	}

	/**
		hl_throw's own hl_debug_break, requested at the entry trap: exc_value now
		holds the thrown vdynamic, readable at last (EIP is already past the VM's
		own int3, so a later continue resumes plainly with no trap dance). True
		when this trap was ours and the stop was reported; false when the trap
		belongs to something else (attach/loader noise).
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
		Frames parked at hl_throw's ENTRY for the stop reported at hl_throw's own
		break (by then execution is deep inside hl_throw, where the frame chain is
		no longer walkable). Consumed on first use; null when nothing is parked.
	**/
	public function consumeParkedFrames(threadId:Int):Null<Array<StackFrameLocation>> {
		var parked = vmThrowFrames.get(threadId);
		if (parked != null) {
			vmThrowFrames.remove(threadId);
		}
		return parked;
	}
}
