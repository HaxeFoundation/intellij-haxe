package debug;

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

	public function new(api:DebugApi, pid:Int) {
		this.api = api;
		this.pid = pid;
	}

	/**
	 * Replaces all breakpoints for `sourceKey` with the given locations. The
	 * caller assigns each location's breakpoint id (so ids stay stable across
	 * re-verification). Returns the installed breakpoints in input order.
	 */
	public function setForSource(sourceKey:String, locations:Array<{id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int}>):Array<PatchedBreakpoint> {
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

	function install(loc:{id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int}):PatchedBreakpoint {
		var key = addressKey(loc.address);
		var existing = byAddress.get(key);
		if (existing != null) {
			// same address already patched (e.g. two source entries collapse); reuse it
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
			line: loc.line
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
