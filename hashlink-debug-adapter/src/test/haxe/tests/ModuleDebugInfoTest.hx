package tests;

import debug.ModuleDebugInfo;

/**
 * Exercises ModuleDebugInfo against the compiled test fixture. The fixture path
 * is provided via DAP_FIXTURE_HL (set by the Gradle testHaxeAdapter task);
 * when unset the suite prints SKIP so a bare `haxe test.hxml` still passes.
 *
 * Line constants mirror test-fixtures/src/Main.hx.
 */
class ModuleDebugInfoTest {
	static inline var FIXTURE_LOOP_LINE = 18;
	static inline var FIXTURE_ADD_LINE = 27;

	public static function run(assert:Assert):Void {
		var fixture = Sys.getEnv("DAP_FIXTURE_HL");
		if (fixture == null || !sys.FileSystem.exists(fixture)) {
			Sys.println("SKIP ModuleDebugInfoTest (DAP_FIXTURE_HL not set or missing)");
			return;
		}

		var module = new ModuleDebugInfo(fixture);
		assert.isTrue(module.functionCount() > 0, "fixture has functions");

		var loopHits = module.resolveLine("Main.hx", FIXTURE_LOOP_LINE);
		assert.isTrue(loopHits.length > 0, "loop line resolves to code");
		assert.equals(FIXTURE_LOOP_LINE, loopHits[0].line, "loop line not moved");

		// reverse lookup of a resolved location returns the same file/line
		var back = module.lookup(loopHits[0].fidx, loopHits[0].op);
		assert.isTrue(back != null, "reverse lookup succeeds");
		assert.equals(FIXTURE_LOOP_LINE, back.line, "reverse lookup line matches");
		assert.isTrue(StringTools.endsWith(normalizeSlashes(back.file), "Main.hx"), "reverse lookup file is Main.hx");

		var addHits = module.resolveLine("Main.hx", FIXTURE_ADD_LINE);
		assert.isTrue(addHits.length > 0, "add line resolves to code");
		var addName = module.functionName(addHits[0].fidx);
		assert.isTrue(StringTools.endsWith(addName, "add"), "add frame name ends with 'add' (was " + addName + ")");

		// absolute path with backslashes should still match (Windows client paths)
		var abs = "C:\\some\\project\\src\\Main.hx";
		assert.isTrue(module.resolveLine(abs, FIXTURE_LOOP_LINE).length > 0, "absolute backslash path matches");

		// a line with no code and no later code -> unresolved
		assert.equals(0, module.resolveLine("NoSuchFile.hx", 5).length, "unknown file unresolved");
	}

	static function normalizeSlashes(p:String):String {
		return p == null ? "" : StringTools.replace(p, "\\", "/");
	}
}
