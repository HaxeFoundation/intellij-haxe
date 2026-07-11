package debug.module;

/**
 * Lists the named locals and arguments visible at a given opcode, resolving each
 * to the bytecode register it occupies, from the function's `assigns` debug table.
 *
 * Encoding (verified against compiled fixtures):
 *  - Arguments have `position < 0`. The named-argument assigns, in table order, map
 *    to the argument registers starting at `argCount - namedArgs` (so an instance
 *    method's unnamed `this` occupies register 0 and the named args follow).
 *  - Locals have `position >= 0`; the register is the destination of the opcode at
 *    that position. A register can be reused for different locals across disjoint
 *    ranges, so the assign with the greatest position ≤ the current opcode wins.
 */
class LocalsResolver {
	final module:ModuleDebugInfo;

	public function new(module:ModuleDebugInfo) {
		this.module = module;
	}

	public function localsAt(fidx:Int, currentOp:Int):Array<LocalVar> {
		var assigns = module.assignsOf(fidx);

		// arguments: the named ones map, in order, onto the trailing argument registers
		var namedArgs = [for (a in assigns) if (a.position < 0) a];
		var argStart = module.argCount(fidx) - namedArgs.length;
		var result:Array<LocalVar> = [];
		for (i in 0...namedArgs.length) {
			result.push({name: module.stringAt(namedArgs[i].varName), register: argStart + i});
		}

		// locals: latest assignment (position <= currentOp) per register wins
		var latestByRegister = new Map<Int, {name:String, position:Int}>();
		for (a in assigns) {
			if (a.position < 0 || a.position > currentOp) {
				continue;
			}
			var register = module.dstRegister(fidx, a.position);
			if (register < 0) {
				continue;
			}
			var existing = latestByRegister.get(register);
			if (existing == null || a.position > existing.position) {
				latestByRegister.set(register, {name: module.stringAt(a.varName), position: a.position});
			}
		}
		for (register => info in latestByRegister) {
			result.push({name: info.name, register: register});
		}

		return result;
	}
}
