package tests;

import ijhaxe.hxcpp.debug.eval.Evaluator;

class EvaluatorTest {
	public static function run(assert:Assert):Void {
		arithmeticAndComparisons(assert);
		readsFrameLocals(assert);
		fieldAndIndexAccess(assert);
		assignmentWritesBackToTheFrame(assert);
		methodCallsMutateTheRealObject(assert);
		staticCallsReachRealCompiledCode(assert);
		constructionAndCallsCompose(assert);
		dottedPackagePathsResolve(assert);
		unknownIdentifiersStillError(assert);
		conditionHoldsIsFailSafe(assert);
	}

	static function make():{eval:Evaluator, api:FakeDebuggerApi} {
		var api = new FakeDebuggerApi();
		return {eval: new Evaluator(api), api: api};
	}

	static function arithmeticAndComparisons(assert:Assert):Void {
		var t = make();
		assert.equals(7, t.eval.evaluate(0, 0, "3 + 4"), "arithmetic");
		assert.equals(true, t.eval.evaluate(0, 0, "2 < 5"), "comparison true");
		assert.equals(false, t.eval.evaluate(0, 0, "10 == 3"), "comparison false");
	}

	static function readsFrameLocals(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 8);
		assert.equals(8, t.eval.evaluate(0, 0, "count"), "reads a local");
		assert.equals(16, t.eval.evaluate(0, 0, "count * 2"), "local in an expression");
		assert.equals(true, t.eval.evaluate(0, 0, "count > 5"), "comparison on a local");
	}

	static function fieldAndIndexAccess(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["p", "nums"];
		t.api.localValues.set("p", {x: 3, y: 4});
		t.api.localValues.set("nums", [10, 20, 30]);
		assert.equals(3, t.eval.evaluate(0, 0, "p.x"), "field access");
		assert.equals(20, t.eval.evaluate(0, 0, "nums[1]"), "array index");
		assert.equals(7, t.eval.evaluate(0, 0, "p.x + p.y"), "compound field expression");
	}

	static function assignmentWritesBackToTheFrame(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 1);
		var result = t.eval.evaluate(0, 0, "count = 99");
		assert.equals(99, result, "assignment returns the stored value");
		assert.equals(1, t.api.setVarCalls.length, "write reached the runtime");
		assert.equals("count", t.api.setVarCalls[0].name, "wrote the right local");
		assert.equals(99, t.api.setVarCalls[0].value, "wrote the evaluated value");
		// `==` is NOT an assignment
		t.eval.evaluate(0, 0, "count == 99");
		assert.equals(1, t.api.setVarCalls.length, "comparison did not write");
	}

	// The user-visible guarantee behind evaluate: hscript is reflection over
	// LIVE references, not a sandbox. A method called on a frame local runs
	// the real compiled method and mutates the real object.
	static function methodCallsMutateTheRealObject(assert:Assert):Void {
		var t = make();
		var box = new EvalTarget(10);
		t.api.localNames = ["box"];
		t.api.localValues.set("box", box);
		assert.equals(17, t.eval.evaluate(0, 0, "box.addTo(7)"), "method call returns the method's result");
		assert.equals(17, box.value, "the REAL object was mutated (same reference)");
		assert.equals(17, t.eval.evaluate(0, 0, "box.value"), "a later evaluate sees the mutation");
		assert.equals(0, t.api.setVarCalls.length, "mutation went through the object, not variable write-back");
	}

	static function staticCallsReachRealCompiledCode(assert:Assert):Void {
		var t = make();
		EvalTarget.total = 0;
		assert.equals(5, t.eval.evaluate(0, 0, "EvalTarget.bump(5)"), "static method call returns");
		assert.equals(5, EvalTarget.total, "real static state was mutated");
		t.eval.evaluate(0, 0, "EvalTarget.bump(2)");
		assert.equals(7, EvalTarget.total, "state persists and accumulates across evaluates");
		assert.equals(7, t.eval.evaluate(0, 0, "EvalTarget.total"), "static field reads back through evaluate");
	}

	static function constructionAndCallsCompose(assert:Assert):Void {
		var t = make();
		assert.equals(9, t.eval.evaluate(0, 0, "new EvalTarget(4).addTo(5)"), "constructs a real instance and calls it");
		assert.equals(4, t.eval.evaluate(0, 0, "Std.int(4.7)"), "std-library statics resolve too");
	}

	static function dottedPackagePathsResolve(assert:Assert):Void {
		var t = make();
		evalfixtures.PackTarget.total = 0;
		evalfixtures.deep.DeepTarget.total = 0;
		assert.equals(4, t.eval.evaluate(0, 0, "evalfixtures.PackTarget.bump(4)"), "packaged static call");
		assert.equals(4, evalfixtures.PackTarget.total, "packaged static state mutated for real");
		assert.equals(6, t.eval.evaluate(0, 0, "new evalfixtures.PackTarget(1).addTo(5)"), "packaged construction");
		assert.equals(3, t.eval.evaluate(0, 0, "evalfixtures.deep.DeepTarget.bump(3)"), "two package levels");
		// two chains sharing the root identifier in ONE expression must merge
		assert.equals(9, t.eval.evaluate(0, 0,
			"evalfixtures.PackTarget.bump(1) + evalfixtures.deep.DeepTarget.bump(1)"), "shared-root chains merge");
		// a frame local shadows a package root of the same name
		t.api.localNames = ["evalfixtures"];
		t.api.localValues.set("evalfixtures", 41);
		assert.equals(42, t.eval.evaluate(0, 0, "evalfixtures + 1"), "a local shadows the package root");
	}

	static function unknownIdentifiersStillError(assert:Assert):Void {
		var t = make();
		var threw = false;
		try {
			t.eval.evaluate(0, 0, "nosuch.thing.here");
		} catch (e:Dynamic) {
			threw = true;
		}
		assert.isTrue(threw, "an unresolvable dotted path still errors (no silent null)");
		assert.isTrue(t.eval.conditionHolds(0, 0, "typoVar == 2"), "unknown identifier in a condition still fails safe");
	}

	static function conditionHoldsIsFailSafe(assert:Assert):Void {
		var t = make();
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 10);
		assert.isTrue(t.eval.conditionHolds(0, 0, "count > 5"), "true condition holds");
		assert.isTrue(!t.eval.conditionHolds(0, 0, "count > 100"), "false condition does not");
		assert.isTrue(t.eval.conditionHolds(0, 0, "@#$ not valid"), "a broken condition FAILS SAFE (stops)");
		assert.isTrue(t.eval.conditionHolds(0, 0, "count + 1"), "a non-Bool result is treated as stop (fail safe)");
	}
}
