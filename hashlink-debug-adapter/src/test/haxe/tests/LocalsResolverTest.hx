package tests;

import debug.module.LocalVar;
import debug.module.LocalsResolver;
import debug.module.ModuleDebugInfo;


/**
 * Exercises LocalsResolver against the compiled fixture (env DAP_FIXTURE_HL).
 * At the loop line, main's locals are total(reg2)/count(reg3)/i(reg6); add's args
 * are current(reg0)/amount(reg1). Constants mirror test-fixtures/src/Main.hx.
 */
class LocalsResolverTest {
	static inline var FIXTURE_LOOP_LINE = 18;
	static inline var FIXTURE_ADD_LINE = 28;
	static inline var FIXTURE_POINT_METHOD_LINE = 22; // Point.move (Point.hx)

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

		// instance method: the unnamed leading argument surfaces as `this` in reg 0,
		// with the named args following
		var moveLoc = module.resolveLine("Point.hx", FIXTURE_POINT_METHOD_LINE)[0];
		var moveLocals = resolver.localsAt(moveLoc.fidx, moveLoc.op);
		assert.equals(0, registerOf(moveLocals, "this"), "this is arg register 0");
		assert.equals(1, registerOf(moveLocals, "dx"), "dx is arg register 1");
		assert.equals(2, registerOf(moveLocals, "dy"), "dy is arg register 2");
	}

	static function registerOf(locals:Array<debug.module.LocalVar>, name:String):Int {
		for (l in locals) {
			if (l.name == name) {
				return l.register;
			}
		}
		return -1;
	}
}
