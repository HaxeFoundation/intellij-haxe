package tests;

import debug.LocalsResolver;
import debug.ModuleDebugInfo;

/**
 * Exercises LocalsResolver against the compiled fixture (env DAP_FIXTURE_HL).
 * At the loop line, main's locals are total(reg2)/count(reg3)/i(reg6); add's args
 * are current(reg0)/amount(reg1). Constants mirror test-fixtures/src/Main.hx.
 */
class LocalsResolverTest {
	static inline var FIXTURE_LOOP_LINE = 18;
	static inline var FIXTURE_ADD_LINE = 27;

	public static function run(assert:Assert):Void {
		var fixture = Sys.getEnv("DAP_FIXTURE_HL");
		if (fixture == null || !sys.FileSystem.exists(fixture)) {
			Sys.println("SKIP LocalsResolverTest (DAP_FIXTURE_HL not set or missing)");
			return;
		}
		var module = new ModuleDebugInfo(fixture);
		var resolver = new LocalsResolver(module);

		var loop = module.resolveLine("Main.hx", FIXTURE_LOOP_LINE)[0];
		var mainLocals = resolver.localsAt(loop.fidx, loop.op);
		assert.equals(2, registerOf(mainLocals, "total"), "total in register 2");
		assert.equals(3, registerOf(mainLocals, "count"), "count in register 3");
		assert.equals(6, registerOf(mainLocals, "i"), "i in register 6");

		var addLoc = module.resolveLine("Main.hx", FIXTURE_ADD_LINE)[0];
		var addLocals = resolver.localsAt(addLoc.fidx, addLoc.op);
		assert.equals(0, registerOf(addLocals, "current"), "current is arg register 0");
		assert.equals(1, registerOf(addLocals, "amount"), "amount is arg register 1");
	}

	static function registerOf(locals:Array<debug.LocalVar>, name:String):Int {
		for (l in locals) {
			if (l.name == name) {
				return l.register;
			}
		}
		return -1;
	}
}
