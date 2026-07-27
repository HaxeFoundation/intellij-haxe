package tests;

import ijhaxe.hxcpp.debug.breakpoints.LineTable;

class LineTableTest {
	public static function run(assert:Assert):Void {
		parsesRowsAndAnswersMembership(assert);
		resolvesBySuffixLikeTheRuntimeKeys(assert);
		unknownFileYieldsNull(assert);
		malformedRowsAreSkipped(assert);
	}

	static function table():LineTable {
		return LineTable.parse("C:/build/src/Main.hx|3,5,9,12\nC:/build/src/pack/Util.hx|2\n");
	}

	static function parsesRowsAndAnswersMembership(assert:Assert):Void {
		var lines = table().linesFor("C:/build/src/Main.hx");
		assert.isTrue(lines != null, "known file has lines");
		assert.isTrue(LineTable.hasLine(lines, 3), "first line found");
		assert.isTrue(LineTable.hasLine(lines, 9), "middle line found");
		assert.isTrue(LineTable.hasLine(lines, 12), "last line found");
		assert.isTrue(!LineTable.hasLine(lines, 4), "gap line rejected");
		assert.isTrue(!LineTable.hasLine(lines, 1), "line before first rejected");
		assert.isTrue(!LineTable.hasLine(lines, 99), "line after last rejected");
	}

	static function resolvesBySuffixLikeTheRuntimeKeys(assert:Assert):Void {
		// a moved checkout still matches its table entry by trailing segments
		var lines = table().linesFor("D:/elsewhere/src/pack/Util.hx");
		assert.isTrue(lines != null, "suffix-matched file resolves");
		assert.isTrue(LineTable.hasLine(lines, 2), "and answers membership");
	}

	static function unknownFileYieldsNull(assert:Assert):Void {
		assert.isTrue(table().linesFor("C:/other/Ghost.hx") == null, "unknown file -> null (degrade)");
	}

	static function malformedRowsAreSkipped(assert:Assert):Void {
		var parsed = LineTable.parse("garbage\n|5\nC:/x/A.hx|\nC:/x/B.hx|7\n");
		assert.isTrue(parsed.linesFor("C:/x/B.hx") != null, "good row survives bad neighbours");
		assert.isTrue(parsed.linesFor("C:/x/A.hx") == null, "row without lines is dropped");
	}
}
