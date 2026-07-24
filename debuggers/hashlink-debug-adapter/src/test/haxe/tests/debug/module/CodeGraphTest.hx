package tests.debug.module;

import ijhaxe.debug.module.CodeGraph;

import format.hl.Data.Opcode;

class CodeGraphTest {
	public static function run(assert:Assert):Void {
		successorsFollowJumpArithmetic(assert);
		successorsForSwitchAndTerminal(assert);
		stepTargetsFindsCallsAndLineChange(assert);
		stepTargetsSkipsTheStartCallWhenItAlreadyRan(assert);
		stepTargetsStopsAtLineChangeAndBranches(assert);
		stepTargetsDetectsReturn(assert);
		guardedThrowFlowsToTheCatchHandler(assert);
		unguardedThrowIsTerminal(assert);
	}

	// A small synthetic function:
	//  0 OInt   line 10   (start)
	//  1 OCall1 line 10   (call on the start line)
	//  2 OSub   line 11   (line change)
	//  3 OJTrue line 11   (branch: fall to 4, or +2 to 6)
	//  4 OMov   line 12
	//  5 ORet   line 12
	//  6 ORet   line 13   (branch target of 3)
	static function fixtureOps():Array<Opcode> {
		return [
			OInt(0, 0),
			OCall1(1, 5, 0),
			OSub(0, 0, 1),
			OJTrue(0, 2),
			OMov(0, 1),
			ORet(0),
			ORet(0)
		];
	}

	static var lines = [10, 10, 11, 11, 12, 12, 13];

	static function lineOf(op:Int):Int {
		return op >= 0 && op < lines.length ? lines[op] : 0;
	}

	static function successorsFollowJumpArithmetic(assert:Assert):Void {
		var g = new CodeGraph(fixtureOps());
		assert.equals("1", g.successors(0).join(","), "OInt falls through to next");
		assert.equals("2", g.successors(1).join(","), "OCall falls through to next");
		// OJTrue at op3: fall-through 4, jump 3+1+2 = 6
		assert.equals("4,6", g.successors(3).join(","), "conditional jump = [next, next+offset]");
		assert.equals("", g.successors(5).join(","), "ORet is terminal");
	}

	static function successorsForSwitchAndTerminal(assert:Assert):Void {
		// index 0 OSwitch(reg, cases=[1,3], end=5): next=1; targets 1, 1+5=6(out of range, dropped),
		// 1+1=2, 1+3=4. Only in-range kept.
		var ops:Array<Opcode> = [OSwitch(0, [1, 3], 5), ONop, ONop, ONop, ONop];
		var g = new CodeGraph(ops);
		var succ = g.successors(0);
		assert.isTrue(succ.indexOf(1) >= 0, "switch fall-through");
		assert.isTrue(succ.indexOf(2) >= 0, "switch case 1 -> next+1");
		assert.isTrue(succ.indexOf(4) >= 0, "switch case 3 -> next+3");
		assert.isFalse(succ.indexOf(6) >= 0, "out-of-range switch target dropped");

		var uncond:Array<Opcode> = [OJAlways(2), ONop, ONop, ONop];
		assert.equals("3", new CodeGraph(uncond).successors(0).join(","), "OJAlways = [next+offset]");
	}

	static function stepTargetsFindsCallsAndLineChange(assert:Assert):Void {
		var g = new CodeGraph(fixtureOps());
		var t = g.stepTargets(0, 10, lineOf);
		assert.equals("2", t.lineChangeOps.join(","), "next line target is first op of line 11");
		assert.equals("1", t.callOps.join(","), "call on the start line is recorded");
		assert.isFalse(t.returns, "no return reachable before the line change");
	}

	// After stepping out of a call, the debuggee parks at the return address —
	// which maps MID-op back onto the call op that just finished. With
	// startOpCallDone that call is not offered again; a fresh stop AT the op
	// (call not yet executed) still offers it.
	static function stepTargetsSkipsTheStartCallWhenItAlreadyRan(assert:Assert):Void {
		var g = new CodeGraph(fixtureOps());
		var t = g.stepTargets(1, 10, lineOf, true);
		assert.equals("", t.callOps.join(","), "the just-returned call is not offered again");
		assert.equals("2", t.lineChangeOps.join(","), "line-change landings are unaffected");

		var fresh = g.stepTargets(1, 10, lineOf);
		assert.equals("1", fresh.callOps.join(","), "a not-yet-executed call at the stop op is still offered");
	}

	static function stepTargetsStopsAtLineChangeAndBranches(assert:Assert):Void {
		var g = new CodeGraph(fixtureOps());
		var t = g.stepTargets(2, 11, lineOf);
		// from op2 (line 11) -> op3 branch -> op4 (line 12) and op6 (line 13)
		t.lineChangeOps.sort((a, b) -> a - b);
		assert.equals("4,6", t.lineChangeOps.join(","), "both branch line-changes found");
		assert.isFalse(t.returns, "walk stopped at line changes before the returns");
	}

	static function stepTargetsDetectsReturn(assert:Assert):Void {
		var g = new CodeGraph(fixtureOps());
		var t = g.stepTargets(4, 12, lineOf);
		assert.equals(0, t.lineChangeOps.length, "no other line reachable from the last line");
		assert.isTrue(t.returns, "ORet on the same line marks a return");
	}

	// try { throw } catch { ... } — the shape of the reported bug:
	//  0 OTrap(end=3) line 20   handler at 0+1+3 = 4
	//  1 OString      line 21   (the throw line: build the value)
	//  2 OThrow       line 21   guarded -> flows to the handler, NOT out
	//  3 OEndTrap     line 22   (normal exit, jumped over on throw)
	//  4 OMov         line 23   catch handler (trace line)
	//  5 ORet         line 24
	static function tryCatchOps():Array<Opcode> {
		return [
			OTrap(0, 3),
			OString(0, 0),
			OThrow(0),
			OEndTrap(true),
			OMov(0, 0),
			ORet(0)
		];
	}

	static var tryCatchLines = [20, 21, 21, 22, 23, 24];

	static function guardedThrowFlowsToTheCatchHandler(assert:Assert):Void {
		var g = new CodeGraph(tryCatchOps());
		assert.equals("4", g.successors(2).join(","), "guarded OThrow's successor is the catch handler");
		assert.isFalse(g.isTerminal(2), "a guarded throw does not leave the function");

		// stepping from the throw line must land on the catch handler's line,
		// and must NOT claim the function returns (the reported bug: no temp at
		// the catch, so the step ran through catch and out to the caller)
		var t = g.stepTargets(1, 21, op -> op >= 0 && op < tryCatchLines.length ? tryCatchLines[op] : 0);
		assert.equals("4", t.lineChangeOps.join(","), "step from the throw line lands at the catch handler");
		assert.isFalse(t.returns, "a caught throw is not a function exit");
	}

	static function unguardedThrowIsTerminal(assert:Assert):Void {
		var ops:Array<Opcode> = [OString(0, 0), OThrow(0)];
		var g = new CodeGraph(ops);
		assert.equals("", g.successors(1).join(","), "unguarded OThrow has no successors");
		assert.isTrue(g.isTerminal(1), "unguarded throw leaves the function");
		var t = g.stepTargets(0, 30, _ -> 30);
		assert.isTrue(t.returns, "stepping over an unguarded throw may leave the function");
	}
}
