package tests.debug.module;

import debug.module.CodeGraph;
import debug.module.LocalScopes;

import format.hl.Data.Opcode;

class LocalScopesTest {
	public static function run(assert:Assert):Void {
		loopVariableShadowsInsideTheLoop(assert);
		outerBindingReturnsAfterTheLoop(assert);
		assignOpItselfDoesNotBindYet(assert);
		branchOnlyVariablesGoOutOfScope(assert);
	}

	// The shape genhl emits for:  var x = "…"; var total = 0; for (x in 0...n) { total += x; }  var s = "…";
	//  0 OInt    x(outer) -> r0
	//  1 OInt    total    -> r1
	//  2 OInt    counter r2 (no assign)
	//  3 OLabel  loop head
	//  4 OJSGte  exit -> 9
	//  5 OMov    x(loop)  -> r3
	//  6 OIncr   counter
	//  7 OAdd    total += x
	//  8 OJAlways back to 3
	//  9 OInt    s -> r4
	// 10 ORet
	static function loopOps():Array<Opcode> {
		return [
			OInt(0, 0),
			OInt(1, 0),
			OInt(2, 0),
			OLabel,
			OJSGte(2, 5, 4),
			OMov(3, 2),
			OIncr(2),
			OAdd(1, 1, 3),
			OJAlways(-6),
			OInt(4, 7),
			ORet(5)
		];
	}

	static function loopScopes():LocalScopes {
		var assigns = [
			{name: "x", position: 0},
			{name: "total", position: 1},
			{name: "x", position: 5},
			{name: "s", position: 9},
		];
		var dst = [0 => 0, 1 => 1, 5 => 3, 9 => 4];
		return new LocalScopes(new CodeGraph(loopOps()), assigns, pos -> dst.exists(pos) ? dst.get(pos) : -1);
	}

	static function registerOf(locals:Array<debug.module.LocalVar>, name:String):Int {
		for (l in locals) {
			if (l.name == name) {
				return l.register;
			}
		}
		return -1;
	}

	static function loopVariableShadowsInsideTheLoop(assert:Assert):Void {
		var locals = loopScopes().visibleLocals(7);
		assert.equals(2, locals.length, "one x + total inside the loop (no duplicate x)");
		assert.equals(3, registerOf(locals, "x"), "the loop x (r3) shadows the outer one");
		assert.equals(1, registerOf(locals, "total"), "total resolved through the loop head");
	}

	static function outerBindingReturnsAfterTheLoop(assert:Assert):Void {
		var locals = loopScopes().visibleLocals(10);
		assert.equals(3, locals.length, "x + total + s after the loop");
		assert.equals(0, registerOf(locals, "x"), "x falls back to the outer binding (r0)");
		assert.equals(4, registerOf(locals, "s"), "s visible once past its assign");
	}

	static function assignOpItselfDoesNotBindYet(assert:Assert):Void {
		// at the loop assign op the new binding hasn't been written yet
		assert.equals(0, loopScopes().registerOf("x", 5), "at the assign op x still means the outer binding");
		assert.equals(-1, loopScopes().registerOf("s", 9), "s not in scope at its own assign op");
	}

	//  0 OInt   w -> r0
	//  1 OJTrue then: fall to 2 / else: -> 4
	//  2 OInt   y -> r2   (then-branch only)
	//  3 OJAlways -> 5
	//  4 OInt   z -> r3   (else-branch only)
	//  5 ORet
	static function branchOnlyVariablesGoOutOfScope(assert:Assert):Void {
		var ops:Array<Opcode> = [
			OInt(0, 0),
			OJTrue(1, 2),
			OInt(2, 5),
			OJAlways(1),
			OInt(3, 6),
			ORet(0)
		];
		var assigns = [
			{name: "w", position: 0},
			{name: "y", position: 2},
			{name: "z", position: 4},
		];
		var dst = [0 => 0, 2 => 2, 4 => 3];
		var scopes = new LocalScopes(new CodeGraph(ops), assigns, pos -> dst.exists(pos) ? dst.get(pos) : -1);
		var locals = scopes.visibleLocals(5);
		assert.equals(1, locals.length, "only w survives the join (y/z were branch-local)");
		assert.equals(0, registerOf(locals, "w"), "w consistent across both branches");
		assert.equals(-1, scopes.registerOf("y", 5), "then-branch var out of scope after the join");
		assert.equals(-1, scopes.registerOf("z", 5), "else-branch var out of scope after the join");
	}
}
