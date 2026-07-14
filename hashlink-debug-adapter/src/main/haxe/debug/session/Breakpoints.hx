package debug.session;

import debug.Pointer;
import debug.module.ExceptionSites.ThrowSite;
import debug.target.DebugApi;

import haxe.Int64;
import haxe.io.Bytes;

/**
 * Installs and tracks software (INT3) breakpoints in the debuggee.
 *
 * All memory access goes through DebugApi, so this class is exercised in tests
 * with a fake API over in-memory bytes. It performs no threading and no event
 * handling; DebugSession drives it from the single debug thread.
 *
 * DAP setBreakpoints replaces the entire breakpoint set for a source file, so
 * breakpoints are grouped by source key and replaced wholesale.
 */
class Breakpoints {
	static inline var INT3 = 0xCC;

	final api:DebugApi;
	final pid:Int;
	final byAddress:Map<String, PatchedBreakpoint> = new Map();
	final bySource:Map<String, Array<PatchedBreakpoint>> = new Map();
	// temporary INT3s planted for a step; `shared` = coincides with a breakpoint
	final temps:Map<String, {address:Pointer, originalByte:Int, shared:Bool}> = new Map();
	// INT3s planted at every throw site while an exception breakpoint is enabled;
	// `reg` is the HL register holding the thrown value at that site
	final byException:Map<String, {bp:PatchedBreakpoint, reg:Int}> = new Map();

	public function new(api:DebugApi, pid:Int) {
		this.api = api;
		this.pid = pid;
	}

	/**
	 * Replaces all breakpoints for `sourceKey` with the given locations. The
	 * caller assigns each location's breakpoint id (so ids stay stable across
	 * re-verification). Returns the installed breakpoints in input order.
	 */
	public function setForSource(sourceKey:String, locations:Array<BreakpointLocation>):Array<PatchedBreakpoint> {
		clearSource(sourceKey);
		var installed:Array<PatchedBreakpoint> = [];
		for (loc in locations) {
			installed.push(install(loc));
		}
		bySource.set(sourceKey, installed);
		return installed;
	}

	/** Removes and restores every breakpoint installed for `sourceKey`. */
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

	/** The breakpoint installed at `address`, or null. */
	public function atAddress(address:Pointer):Null<PatchedBreakpoint> {
		return byAddress.get(addressKey(address));
	}

	public function isBreakpointAddress(address:Pointer):Bool {
		return byAddress.exists(addressKey(address));
	}

	/** All installed breakpoints across every source. */
	public function all():Array<PatchedBreakpoint> {
		return [for (bp in byAddress) bp];
	}

	/** Temporarily restores the original byte (before stepping over the breakpoint). */
	public function suspend(bp:PatchedBreakpoint):Void {
		writeByte(bp.address, bp.originalByte);
	}

	/** Re-installs the INT3 after a step-over. */
	public function rearm(bp:PatchedBreakpoint):Void {
		writeByte(bp.address, INT3);
	}

	/**
	 * Plants a temporary INT3 (for a step) at `address`. If a breakpoint is
	 * already installed there, nothing is written and the temp is marked `shared`
	 * so clearTemps leaves the breakpoint intact.
	 */
	public function addTemp(address:Pointer):Void {
		var key = addressKey(address);
		if (temps.exists(key)) {
			return;
		}
		// a breakpoint OR an armed throw-site already holds an INT3 here — leave
		// its byte alone so clearTemps doesn't restore over the wrong original
		var shared = byAddress.exists(key) || byException.exists(key);
		var original = INT3;
		if (!shared) {
			original = readByte(address);
			writeByte(address, INT3);
		}
		temps.set(key, {address: address, originalByte: original, shared: shared});
	}

	/** Removes all temporary breakpoints, restoring bytes not shared with a breakpoint. */
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

	/** Restores a single temp's original byte (to single-step past it at a wrong frame). */
	public function suspendTemp(address:Pointer):Void {
		var temp = temps.get(addressKey(address));
		if (temp != null && !temp.shared) {
			writeByte(address, temp.originalByte);
		}
	}

	/** Re-installs a single temp's INT3 after stepping past it. */
	public function rearmTemp(address:Pointer):Void {
		var temp = temps.get(addressKey(address));
		if (temp != null && !temp.shared) {
			writeByte(address, INT3);
		}
	}

	public function hasTemps():Bool {
		return temps.keys().hasNext();
	}

	// --- exception breakpoints (one INT3 per throw site while enabled) ---

	/** Plants an INT3 at every throw site (skipping addresses already patched). */
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

	/** Restores every throw-site byte and forgets them. */
	public function disarmExceptions():Void {
		for (entry in byException) {
			restore(entry.bp);
		}
		byException.clear();
	}

	public function isExceptionsArmed():Bool {
		return byException.keys().hasNext();
	}

	/** The throw-site breakpoint at `address` (with its thrown-value register), or null. */
	public function exceptionAt(address:Pointer):Null<{bp:PatchedBreakpoint, reg:Int}> {
		return byException.get(addressKey(address));
	}

	/**
	 * Restores every patched byte (breakpoints and temps). Used before
	 * detaching in attach mode: the debuggee keeps running without a debugger,
	 * so any leftover INT3 would crash it. Restoring a byte that was already
	 * suspended writes the same original value again — harmless.
	 */
	public function removeAll():Void {
		clearTemps();
		disarmExceptions();
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
			// same address already patched (e.g. two source entries collapse); the
			// latest request's condition wins so an edited condition takes effect
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
