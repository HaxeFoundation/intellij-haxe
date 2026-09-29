package ijhaxe.debug.breakpoints;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.module.ExceptionSites.ThrowSite;
import ijhaxe.debug.target.DebugApi;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Installs and tracks software (INT3) breakpoints in the debuggee.

	All memory access goes through DebugApi, so tests run this class against a
	fake API over in-memory bytes. It does no threading and no event handling;
	DebugSession drives it from the session thread.

	A DAP setBreakpoints request replaces all breakpoints of a source file, so
	breakpoints are grouped by source key and replaced as a whole.
**/
class Breakpoints {
	static inline var INT3 = 0xCC;

	final api:DebugApi;
	final pid:Int;
	final byAddress:Map<String, PatchedBreakpoint> = new Map();
	final bySource:Map<String, Array<PatchedBreakpoint>> = new Map();
	// temporary INT3s planted for a step; `shared` marks one at the address of
	// a breakpoint or throw site, whose INT3 the temp reuses
	final temps:Map<String, {address:Pointer, originalByte:Int, shared:Bool}> = new Map();
	// INT3s at every throw site while exception breakpoints are armed; `reg` is
	// the HL register holding the thrown value at that site
	final byException:Map<String, {bp:PatchedBreakpoint, reg:Int}> = new Map();
	// One INT3 on hl_throw's entry while the "vm" exception filter is on. It
	// catches VM-raised errors (null access, out of bounds) that execute no
	// OThrow. Null when disarmed.
	var nativeThrow:Null<{address:Pointer, originalByte:Int}> = null;

	public function new(api:DebugApi, pid:Int) {
		this.api = api;
		this.pid = pid;
	}

	/**
		Replaces all breakpoints for `sourceKey` with the given locations. The
		caller assigns each location's breakpoint id (so ids stay stable across
		re-verification). Returns the installed breakpoints in input order.
	**/
	public function setForSource(sourceKey:String, locations:Array<BreakpointLocation>):Array<PatchedBreakpoint> {
		clearSource(sourceKey);
		var installed:Array<PatchedBreakpoint> = [];
		for (loc in locations) {
			installed.push(install(loc));
		}
		bySource.set(sourceKey, installed);
		return installed;
	}

	/**
		Removes and restores every breakpoint installed for `sourceKey`.
	**/
	public function clearSource(sourceKey:String):Void {
		var existing = bySource.get(sourceKey);
		if (existing == null) {
			return;
		}
		for (bp in existing) {
			restore(bp);
			byAddress.remove(addressKey(bp.address));
		}
		bySource.remove(sourceKey);
	}

	/**
		The breakpoint installed at `address`, or null.
	**/
	public function atAddress(address:Pointer):Null<PatchedBreakpoint> {
		return byAddress.get(addressKey(address));
	}

	/**
		True when one of the session's own INT3s sits at `address`: a breakpoint,
		a step temp, an armed throw site or hl_throw's armed entry. A trap
		anywhere else is foreign (a forced break, the attach or loader breakpoint).
	**/
	public function isPatchedSite(address:Pointer):Bool {
		return atAddress(address) != null || isTemp(address) || exceptionAt(address) != null || isNativeThrow(address);
	}

	/**
		Restores the original byte, so the instruction can run when stepping over the breakpoint.
	**/
	public function suspend(bp:PatchedBreakpoint):Void {
		writeByte(bp.address, bp.originalByte);
	}

	/**
		Re-installs the INT3 after a step-over.
	**/
	public function rearm(bp:PatchedBreakpoint):Void {
		writeByte(bp.address, INT3);
	}

	/**
		Plants a temporary INT3 for a step at `address`. When a breakpoint or an
		armed throw site already has an INT3 there, nothing is written and the
		temp is marked `shared`, so clearTemps leaves that INT3 in place.
	**/
	public function addTemp(address:Pointer):Void {
		var key = addressKey(address);
		if (temps.exists(key)) {
			return;
		}
		// the byte here is already an INT3, not the original instruction byte
		var shared = byAddress.exists(key) || byException.exists(key);
		var original = INT3;
		if (!shared) {
			original = readByte(address);
			writeByte(address, INT3);
		}
		temps.set(key, {address: address, originalByte: original, shared: shared});
	}

	/**
		Removes all temps, restoring every byte a temp does not share.
	**/
	public function clearTemps():Void {
		for (temp in temps) {
			if (!temp.shared) {
				writeByte(temp.address, temp.originalByte);
			}
		}
		temps.clear();
	}

	public function isTemp(address:Pointer):Bool {
		return temps.exists(addressKey(address));
	}

	/**
		Restores one temp's original byte, to single-step past a hit that is not the landing.
	**/
	public function suspendTemp(address:Pointer):Void {
		var temp = temps.get(addressKey(address));
		if (temp != null && !temp.shared) {
			writeByte(address, temp.originalByte);
		}
	}

	/**
		Re-installs one temp's INT3 after stepping past it.
	**/
	public function rearmTemp(address:Pointer):Void {
		var temp = temps.get(addressKey(address));
		if (temp != null && !temp.shared) {
			writeByte(address, INT3);
		}
	}

	public function hasTemps():Bool {
		return temps.keys().hasNext();
	}

	// --- exception breakpoints (one INT3 per throw site while armed) ---

	/**
		Plants an INT3 at every throw site (skipping addresses already patched).
	**/
	public function armExceptions(sites:Array<ThrowSite>):Void {
		for (site in sites) {
			var key = addressKey(site.address);
			if (byAddress.exists(key) || byException.exists(key)) {
				continue; // a breakpoint or an already-armed site owns this byte
			}
			var original = readByte(site.address);
			var bp:PatchedBreakpoint = {
				id: -1, address: site.address, originalByte: original,
				fidx: site.fidx, op: site.op, file: "", line: 0, condition: null
			};
			writeByte(site.address, INT3);
			byException.set(key, {bp: bp, reg: site.reg});
		}
	}

	/**
		Restores every throw-site byte and forgets them.
	**/
	public function disarmExceptions():Void {
		for (entry in byException) {
			restore(entry.bp);
		}
		byException.clear();
	}

	public function isExceptionsArmed():Bool {
		return byException.keys().hasNext();
	}

	/**
		The throw-site breakpoint at `address` (with its thrown-value register), or null.
	**/
	public function exceptionAt(address:Pointer):Null<{bp:PatchedBreakpoint, reg:Int}> {
		return byException.get(addressKey(address));
	}

	// --- the "vm" filter's trap (one INT3 on hl_throw's entry) ---

	/**
		Plants an INT3 at hl_throw's entry (no-op if already armed there).
	**/
	public function armNativeThrow(address:Pointer):Void {
		if (nativeThrow != null) {
			return;
		}
		var original = readByte(address);
		writeByte(address, INT3);
		nativeThrow = {address: address, originalByte: original};
	}

	/**
		Restores hl_throw's entry byte and forgets the trap.
	**/
	public function disarmNativeThrow():Void {
		if (nativeThrow != null) {
			writeByte(nativeThrow.address, nativeThrow.originalByte);
			nativeThrow = null;
		}
	}

	public function isNativeThrowArmed():Bool {
		return nativeThrow != null;
	}

	/**
		True when `address` is hl_throw's armed entry.
	**/
	public function isNativeThrow(address:Pointer):Bool {
		return nativeThrow != null && Int64.eq(nativeThrow.address, address);
	}

	/**
		The hl_throw entry trap as a PatchedBreakpoint, so a stop on it goes
		through the same currentStoppedBreakpoint handling as any breakpoint
		(suspend, rearm, step over on continue). It is not in byAddress, so
		atAddress (and thus reconcileStoppedBreakpoint) ignores it. Null when
		disarmed.
	**/
	public function nativeThrowBreakpoint():Null<PatchedBreakpoint> {
		if (nativeThrow == null) {
			return null;
		}
		return {
			id: -1, address: nativeThrow.address, originalByte: nativeThrow.originalByte,
			fidx: -1, op: -1, file: "", line: 0, condition: null
		};
	}

	/**
		Lifts every planted INT3 (user breakpoints, throw sites, the hl_throw
		trap) without forgetting it, so an injected eval-call runs unpatched
		code. A called function that throws and catches internally, or passes a
		user breakpoint, must not trip the adapter's traps and derail the call.
		Paired with rearmAll(). Step temps stay, because an eval-call runs from a
		stop, not mid-step. Calling it twice is harmless.
	**/
	public function suspendAll():Void {
		for (bp in byAddress) {
			writeByte(bp.address, bp.originalByte);
		}
		for (entry in byException) {
			writeByte(entry.bp.address, entry.bp.originalByte);
		}
		if (nativeThrow != null) {
			writeByte(nativeThrow.address, nativeThrow.originalByte);
		}
	}

	/**
		Re-plants every INT3 lifted by suspendAll, except at `keepSuspended`: the
		breakpoint of the current stop. The continue machinery keeps its original
		byte restored until it single-steps past it; re-arming it here would make
		that step trap on itself.
	**/
	public function rearmAll(?keepSuspended:Pointer):Void {
		for (bp in byAddress) {
			if (keepSuspended == null || !Int64.eq(bp.address, keepSuspended)) {
				writeByte(bp.address, INT3);
			}
		}
		for (entry in byException) {
			if (keepSuspended == null || !Int64.eq(entry.bp.address, keepSuspended)) {
				writeByte(entry.bp.address, INT3);
			}
		}
		if (nativeThrow != null && (keepSuspended == null || !Int64.eq(nativeThrow.address, keepSuspended))) {
			writeByte(nativeThrow.address, INT3);
		}
	}

	/**
		Restores every patched byte: breakpoints, temps, throw sites and the
		hl_throw trap. Used before detaching in attach mode, where the debuggee
		keeps running without a debugger and any leftover INT3 would crash it.
		Restoring a suspended byte writes the same original value again, which
		is harmless.
	**/
	public function removeAll():Void {
		clearTemps();
		disarmExceptions();
		disarmNativeThrow();

		for (bp in byAddress) {
			restore(bp);
		}
		byAddress.clear();
		bySource.clear();
	}

	function install(loc:BreakpointLocation):PatchedBreakpoint {
		var key = addressKey(loc.address);
		var existing = byAddress.get(key);
		if (existing != null) {
			// the address is already patched (two source entries map to it); the
			// latest condition wins, so an edited condition takes effect
			existing.condition = loc.condition;
			return existing;
		}
		var original = readByte(loc.address);
		var bp:PatchedBreakpoint = {
			id: loc.id,
			address: loc.address,
			originalByte: original,
			fidx: loc.fidx,
			op: loc.op,
			file: loc.file,
			line: loc.line,
			condition: loc.condition
		};
		writeByte(loc.address, INT3);
		byAddress.set(key, bp);
		return bp;
	}

	function restore(bp:PatchedBreakpoint):Void {
		writeByte(bp.address, bp.originalByte);
	}

	function readByte(address:Pointer):Int {
		var buf = Bytes.alloc(1);
		api.readMemory(pid, address, buf, 1);
		return buf.get(0);
	}

	function writeByte(address:Pointer, value:Int):Void {
		var buf = Bytes.alloc(1);
		buf.set(0, value);
		api.writeMemory(pid, address, buf, 1);
		api.flush(pid, address, 1);
	}

	static inline function addressKey(address:Pointer):String {
		return Int64.toStr(address);
	}
}
