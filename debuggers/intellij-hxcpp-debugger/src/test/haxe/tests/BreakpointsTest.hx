package tests;

import ijhaxe.dap.protocol.SourceBreakpoint;
import ijhaxe.hxcpp.debug.breakpoints.Breakpoints;
import ijhaxe.hxcpp.debug.breakpoints.LineTable;

class BreakpointsTest {
	public static function run(assert:Assert):Void {
		installsMatchedBreakpointsAndVerifies(assert);
		anUnmatchedSourceIsUnverifiedNotAnError(assert);
		reinstallReplacesTheWholeSource(assert);
		runtimeNumberMapsBackToDapId(assert);
		aLineWithoutCodeIsRejected(assert);
		aFileUnknownToTheTableDegradesToFileLevel(assert);
	}

	static function withFiles(?lineTable:LineTable):{bp:Breakpoints, api:FakeDebuggerApi} {
		var api = new FakeDebuggerApi();
		api.cannedFilesFullPath = ["C:/build/src/Main.hx"];
		api.cannedFiles = ["Main.hx"];
		return {bp: new Breakpoints(api, lineTable), api: api};
	}

	static function bp(line:Int):SourceBreakpoint {
		return {line: line};
	}

	static function installsMatchedBreakpointsAndVerifies(assert:Assert):Void {
		var t = withFiles();
		var results = t.bp.setForSource("D:/moved/src/Main.hx", [bp(10), bp(20)], [1, 2]);
		assert.equals(2, results.length, "one result per request");
		assert.isTrue(results[0].verified, "matched breakpoint verified");
		assert.equals(2, t.api.installedBreakpoints.length, "two breakpoints installed in the runtime");
		assert.equals("Main.hx", t.api.installedBreakpoints[0].file, "installed against the runtime file key");
		assert.equals(20, t.api.installedBreakpoints[1].line, "line carried through");
	}

	static function anUnmatchedSourceIsUnverifiedNotAnError(assert:Assert):Void {
		var t = withFiles();
		var results = t.bp.setForSource("C:/other/Ghost.hx", [bp(5)], [7]);
		assert.equals(1, results.length, "still one result");
		assert.isTrue(results[0].verified == false, "unmatched source is unverified");
		assert.equals(0, t.api.installedBreakpoints.length, "nothing installed for an unmatched source");
	}

	static function reinstallReplacesTheWholeSource(assert:Assert):Void {
		var t = withFiles();
		t.bp.setForSource("C:/build/src/Main.hx", [bp(10)], [1]);
		var firstNumber = t.api.installedBreakpoints[0].number;
		t.bp.setForSource("C:/build/src/Main.hx", [bp(30)], [2]);
		assert.equals(firstNumber, t.api.deletedBreakpoints[0], "the previous breakpoint was deleted");
		assert.equals(30, t.api.installedBreakpoints[1].line, "the replacement was installed");
	}

	static function runtimeNumberMapsBackToDapId(assert:Assert):Void {
		var t = withFiles();
		t.bp.setForSource("C:/build/src/Main.hx", [bp(10)], [42]);
		var runtimeNumber = t.api.installedBreakpoints[0].number;
		assert.equals(42, t.bp.idForRuntimeNumber(runtimeNumber), "runtime number resolves to the DAP id");
		assert.equals(-1, t.bp.idForRuntimeNumber(9999), "unknown runtime number -> -1");
	}

	static function aLineWithoutCodeIsRejected(assert:Assert):Void {
		var t = withFiles(LineTable.parse("C:/build/src/Main.hx|10,20\n"));
		var results = t.bp.setForSource("C:/build/src/Main.hx", [bp(10), bp(15)], [1, 2]);
		assert.isTrue(results[0].verified, "code line verified");
		assert.isTrue(results[1].verified == false, "non-code line rejected");
		assert.equals(15, results[1].line, "the rejected result keeps the requested line");
		assert.isTrue(results[1].message.indexOf("no executable code") >= 0, "rejection names the reason");
		assert.equals(1, t.api.installedBreakpoints.length, "nothing installed for the rejected line");
		assert.equals(10, t.api.installedBreakpoints[0].line, "only the code line installed");
	}

	static function aFileUnknownToTheTableDegradesToFileLevel(assert:Assert):Void {
		// the runtime knows the file but the table does not: never reject on
		// missing knowledge, keep the old file-level behaviour
		var t = withFiles(LineTable.parse("C:/build/src/Other.hx|1\n"));
		var results = t.bp.setForSource("C:/build/src/Main.hx", [bp(15)], [1]);
		assert.isTrue(results[0].verified, "unknown-to-table file stays file-level verified");
		assert.equals(1, t.api.installedBreakpoints.length, "and the breakpoint is installed");
	}
}
