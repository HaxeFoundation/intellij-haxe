package debug;

import format.hl.Data.Opcode;

/**
 * Control-flow graph over one HashLink function's opcodes, used to compute where
 * a source-level step should plant temporary breakpoints.
 *
 * Successor arithmetic (verified against hashlink and vshaxe/hashlink-debugger):
 * a jump's target opcode is `opIndex + 1 + offset`. Returns/throws are terminal.
 * Successor sets are deliberately over-approximated (an unreachable target only
 * costs a temporary breakpoint that is cleaned up), never under-approximated.
 *
 * Pure and dependency-light: operates on an opcode array plus a `lineOf` callback,
 * so it is exercised with synthetic opcodes under the interpreter.
 */
class CodeGraph {
	final ops:Array<Opcode>;

	public function new(ops:Array<Opcode>) {
		this.ops = ops;
	}

	public function opCount():Int {
		return ops.length;
	}

	/** Opcode indices that may execute immediately after opcode `op`. */
	public function successors(op:Int):Array<Int> {
		if (op < 0 || op >= ops.length) {
			return [];
		}
		var next = op + 1;
		var result:Array<Int> = switch (ops[op]) {
			case ORet(_), OThrow(_), ORethrow(_):
				[];
			case OJAlways(d):
				[next + d];
			case OJTrue(_, d), OJFalse(_, d), OJNull(_, d), OJNotNull(_, d):
				[next, next + d];
			case OJSLt(_, _, d), OJSGte(_, _, d), OJSGt(_, _, d), OJSLte(_, _, d),
				OJULt(_, _, d), OJUGte(_, _, d), OJNotLt(_, _, d), OJNotGte(_, _, d),
				OJEq(_, _, d), OJNotEq(_, _, d):
				[next, next + d];
			case OSwitch(_, cases, end):
				var targets = [next, next + end];
				for (c in cases) {
					targets.push(next + c);
				}
				targets;
			case OTrap(_, end):
				[next, next + end];
			default:
				[next];
		}
		return [for (t in result) if (t >= 0 && t < ops.length) t];
	}

	/** True if opcode `op` invokes another function (a step-in candidate). */
	public function isCall(op:Int):Bool {
		if (op < 0 || op >= ops.length) {
			return false;
		}
		return switch (ops[op]) {
			case OCall0(_, _), OCall1(_, _, _), OCall2(_, _, _, _), OCall3(_, _, _, _, _),
				OCall4(_, _, _, _, _, _), OCallN(_, _, _), OCallMethod(_, _, _),
				OCallThis(_, _, _), OCallClosure(_, _, _):
				true;
			default:
				false;
		}
	}

	/** True if opcode `op` ends the function (return/throw). */
	public function isTerminal(op:Int):Bool {
		if (op < 0 || op >= ops.length) {
			return false;
		}
		return switch (ops[op]) {
			case ORet(_), OThrow(_), ORethrow(_): true;
			default: false;
		}
	}

	/**
	 * Walks the CFG from `startOp` collecting step targets. `lineOf(op)` gives the
	 * source line of an opcode (0/negative = unknown). The walk stops expanding at
	 * any opcode whose known line differs from `startLine` (that opcode is a
	 * line-change target) and at terminals; it is guarded against loops.
	 */
	public function stepTargets(startOp:Int, startLine:Int, lineOf:Int->Int):StepTargets {
		var lineChangeOps:Array<Int> = [];
		var callOps:Array<Int> = [];
		var returns = false;

		var visited = new Map<Int, Bool>();
		var pending = [startOp];
		while (pending.length > 0) {
			var op = pending.pop();
			if (op < 0 || op >= ops.length || visited.exists(op)) {
				continue;
			}
			visited.set(op, true);

			if (op != startOp) {
				var line = lineOf(op);
				if (line > 0 && line != startLine) {
					lineChangeOps.push(op);
					continue; // a new line: stop here, don't walk past it
				}
			}

			if (isCall(op)) {
				callOps.push(op);
			}
			if (isTerminal(op)) {
				returns = true;
				continue;
			}
			for (successor in successors(op)) {
				pending.push(successor);
			}
		}

		return {lineChangeOps: lineChangeOps, callOps: callOps, returns: returns};
	}
}
