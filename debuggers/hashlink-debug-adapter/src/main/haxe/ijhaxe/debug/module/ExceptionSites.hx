package ijhaxe.debug.module;

import ijhaxe.debug.Pointer;
import format.hl.Data.Opcode;

/**
	A throw site: the machine address of an `OThrow`/`ORethrow` opcode and the HL
	register holding the value being thrown there.
**/
typedef ThrowSite = {address:Pointer, fidx:Int, op:Int, reg:Int};

/**
	Enumerates every `OThrow`/`ORethrow` opcode in the program to its JIT machine
	address, so an "exception breakpoint" can plant an INT3 at each one. This is a
	CODE-only pass: it walks the already-decoded function opcode lists
	(`data.functions[*].ops`) and never touches the data/constants/bytes sections
	where embedded assets live. The result is computed once and cached — the loop
	is over in-memory opcodes (no re-parsing), and nothing is planted until an
	exception breakpoint is actually enabled.
**/
class ExceptionSites {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	var cached:Null<Array<ThrowSite>> = null;

	public function new(module:ModuleDebugInfo, jit:JitInfo) {
		this.module = module;
		this.jit = jit;
	}

	/**
		All throw sites in the program (cached after the first call).
	**/
	public function all():Array<ThrowSite> {
		if (cached != null) {
			return cached;
		}
		var sites:Array<ThrowSite> = [];
		for (fidx in 0...module.functionCount()) {
			var ops = module.opcodes(fidx);
			for (op in 0...ops.length) {
				var reg = throwRegister(ops[op]);
				if (reg >= 0) {
					sites.push({address: jit.addressOf(fidx, op), fidx: fidx, op: op, reg: reg});
				}
			}
		}
		cached = sites;
		return sites;
	}

	// The thrown-value register of a throw opcode, or -1 when the opcode is not a
	// throw. ORethrow (re-throw of a caught exception) counts too.
	static inline function throwRegister(op:Opcode):Int {
		return switch (op) {
			case OThrow(reg): reg;
			case ORethrow(reg): reg;
			default: -1;
		}
	}
}
