package intellij.hxcpp.debug.breakpoints;

import dap.protocol.SourceBreakpoint;
import dap.protocol.Breakpoint;
import intellij.hxcpp.debug.DebuggerApi;

/** One installed breakpoint: the DAP id we assigned + the runtime's number. */
private typedef Installed = {
	var id:Int;
	var runtimeNumber:Int;
	var line:Int;
	var condition:Null<String>; // evaluated at each hit when non-null (M5)
}

/**
	Owns the source line breakpoints. DAP `setBreakpoints` REPLACES the whole
	set for a source, so each call clears that source's installed breakpoints
	and reinstalls from the request. File resolution goes through FileMatcher
	(suffix match onto the runtime's file key); a source that matches no runtime
	file yields unverified results rather than an error, so the IDE shows a
	hollow marker instead of failing.

	Lines are verified against the macro-baked LineTable: a line without
	executable code is rejected (unverified, with a message) — the usual cause
	is a stale binary. Without a table (older lib build) verification degrades
	to file level. Conditions are carried but not evaluated until M5.
**/
class Breakpoints {
	final debugger:DebuggerApi;
	final lineTable:Null<LineTable>;
	// sourceKey (lower-cased path) -> its installed breakpoints
	final bySource:Map<String, Array<Installed>> = new Map();
	var matcher:Null<FileMatcher> = null;

	public function new(debugger:DebuggerApi, ?lineTable:LineTable) {
		this.debugger = debugger;
		this.lineTable = lineTable;
	}

	/**
		Replaces the breakpoints for `sourcePath` with `requested`, assigning
		each the id from `ids` (same length/order). Returns one DAP Breakpoint
		result per request, in order.
	**/
	public function setForSource(sourcePath:String, requested:Array<SourceBreakpoint>, ids:Array<Int>):Array<Breakpoint> {
		clearSource(sourcePath);
		var fileKey = fileMatcher().resolve(sourcePath);
		var knownLines = lineTable != null ? lineTable.linesFor(sourcePath) : null;
		var installed:Array<Installed> = [];
		var results:Array<Breakpoint> = [];
		for (i in 0...requested.length) {
			var line = requested[i].line;
			if (fileKey == null) {
				results.push({id: ids[i], verified: false, line: line, message: "no matching source file in the debuggee"});
			} else if (knownLines != null && !LineTable.hasLine(knownLines, line)) {
				results.push({id: ids[i], verified: false, line: line, message: "no executable code at this line (stale build?)"});
			} else {
				var runtimeNumber = debugger.addFileLineBreakpoint(fileKey, line);
				installed.push({id: ids[i], runtimeNumber: runtimeNumber, line: line, condition: requested[i].condition});
				results.push({id: ids[i], verified: true, line: line});
			}
		}
		if (installed.length > 0) {
			bySource.set(sourceKey(sourcePath), installed);
		}
		return results;
	}

	/** The condition of the breakpoint installed at runtime `number`, or null. */
	public function conditionForRuntimeNumber(number:Int):Null<String> {
		for (source in bySource) {
			for (bp in source) {
				if (bp.runtimeNumber == number) {
					return bp.condition;
				}
			}
		}
		return null;
	}

	/** The DAP id of the breakpoint installed at runtime `number`, or -1. */
	public function idForRuntimeNumber(number:Int):Int {
		for (source in bySource) {
			for (bp in source) {
				if (bp.runtimeNumber == number) {
					return bp.id;
				}
			}
		}
		return -1;
	}

	function clearSource(sourcePath:String):Void {
		var key = sourceKey(sourcePath);
		var existing = bySource.get(key);
		if (existing != null) {
			for (bp in existing) {
				debugger.deleteBreakpoint(bp.runtimeNumber);
			}
			bySource.remove(key);
		}
	}

	// The runtime's file tables are stable once the program is loaded, so the
	// matcher is built once on first use.
	function fileMatcher():FileMatcher {
		if (matcher == null) {
			matcher = new FileMatcher(debugger.filesFullPath(), debugger.files());
		}
		return matcher;
	}

	static inline function sourceKey(path:String):String {
		return path.toLowerCase();
	}
}
