package ijhaxe.hxcpp.debug.breakpoints;

import ijhaxe.dap.protocol.SourceBreakpoint;
import ijhaxe.dap.protocol.Breakpoint;
import ijhaxe.hxcpp.debug.DebuggerApi;

/** One installed breakpoint: its DAP id and the runtime's breakpoint number. */
private typedef InstalledBreakpoint = {
	var id:Int;
	var runtimeNumber:Int;
	var line:Int;
	var condition:Null<String>; // the Dispatcher evaluates it at each hit when non-null
}

/**
	Owns the line breakpoints of every source. DAP `setBreakpoints` REPLACES
	the whole set of a source, so each call deletes that source's installed
	breakpoints and installs the requested ones. FileMatcher maps the source
	path onto the runtime's file key by path suffix. A source that matches no
	runtime file gets unverified results rather than an error, so the IDE shows
	a hollow marker.

	Requested lines are checked against the LineTable compiled into the
	binary. A line without executable code is rejected (unverified, with a
	message); the usual cause is a stale binary. Without a table (a binary
	built with an older version of this library), only the file is checked.
**/
class Breakpoints {
	final debugger:DebuggerApi;
	final lineTable:Null<LineTable>;
	// sourceKey (lower-cased path) -> its installed breakpoints
	final bySource:Map<String, Array<InstalledBreakpoint>> = new Map();
	var matcher:Null<FileMatcher> = null;

	public function new(debugger:DebuggerApi, ?lineTable:LineTable) {
		this.debugger = debugger;
		this.lineTable = lineTable;
	}

	/**
		Replaces the breakpoints of `sourcePath` with `requested`. Each gets the
		id at the same position in `ids`. Returns one DAP Breakpoint result per
		requested breakpoint, in order.
	**/
	public function setForSource(sourcePath:String, requested:Array<SourceBreakpoint>, ids:Array<Int>):Array<Breakpoint> {
		clearSource(sourcePath);
		var fileKey = fileMatcher().resolve(sourcePath);
		var knownLines = lineTable != null ? lineTable.linesFor(sourcePath) : null;

		var installed:Array<InstalledBreakpoint> = [];
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

	// The runtime's file tables do not change once the program is loaded, so
	// the matcher is built once, on first use.
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
