package ijhaxe.debug.module;

import format.hl.Data.Opcode;

/**
	The control-flow graph of one HashLink function's opcodes. Stepping uses it
	to decide where to plant temporary breakpoints.

	A jump's target opcode is `opIndex + 1 + offset`, as in hashlink and
	vshaxe/hashlink-debugger. A return ends the function. A throw inside a
	`try` does not: the VM jumps (longjmp) to the enclosing OTrap's catch
	handler, so the handlers are its successors, and only an unguarded throw
	ends the function. Successor sets may contain too many targets, never too
	few; an unreachable target only costs a temporary breakpoint that is
	removed again.
**/
class CodeGraph {
	final ops:Array<Opcode>;
	// The `try` ranges, derived as in TryRegions: an OTrap at `start` with
	// offset `d` guards the ops start < op <= start+d, and its catch handler is
	// at start+d+1. Built on the first query about a throw.
	var trapRegions:Null<Array<{start:Int, end:Int}>> = null;

	public function new(ops:Array<Opcode>) {
		this.ops = ops;
	}

	public function opCount():Int {
		return ops.length;
	}

	/**
		Opcode indices that may execute immediately after opcode `op`.
	**/
	public function successors(op:Int):Array<Int> {
		if (op < 0 || op >= ops.length) {
			return [];
		}
		var next = op + 1;
		var result:Array<Int> = switch (ops[op]) {
			case ORet(_):
				[];
			case OThrow(_), ORethrow(_):
				// continues at the catch handlers of the enclosing trys; none means it leaves the function
				catchHandlers(op);
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

	/**
		True if opcode `op` invokes another function (a step-in candidate).
	**/
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

	/**
		True if opcode `op` ends the function: a return, or a throw outside any `try`.
	**/
	public function isTerminal(op:Int):Bool {
		if (op < 0 || op >= ops.length) {
			return false;
		}
		return switch (ops[op]) {
			case ORet(_): true;
			case OThrow(_), ORethrow(_): catchHandlers(op).length == 0;
			default: false;
		}
	}

	// The catch handlers of every `try` range enclosing `op`. At run time only
	// the innermost active handler catches; returning all of them is a safe
	// over-approximation for stepping.
	function catchHandlers(op:Int):Array<Int> {
		if (trapRegions == null) {
			trapRegions = [];
			for (i in 0...ops.length) {
				switch (ops[i]) {
					case OTrap(_, end):
						trapRegions.push({start: i, end: i + end});
					default:
				}
			}
		}
		return [for (r in trapRegions) if (op > r.start && op <= r.end) r.end + 1];
	}

	/**
		Walks the graph from `startOp` and collects the step targets. `lineOf(op)`
		gives an opcode's source line, 0 or negative when unknown. The walk does
		not continue past an opcode on a different known line (it becomes a
		line-change target) or past an opcode that ends the function. Each
		opcode is visited once, so loops terminate.

		`startOpCallDone` is true when the debuggee stopped in the middle of
		`startOp`, at a return address inside its machine code. If `startOp` is
		a call, that call has already run and is not a step-in target again.
		This happens after stepping out of a call.
	**/
	public function stepTargets(startOp:Int, startLine:Int, lineOf:Int->Int,
			startOpCallDone:Bool = false):StepTargets {
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
					continue;
				}
			}

			if (isCall(op) && !(startOpCallDone && op == startOp)) {
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
