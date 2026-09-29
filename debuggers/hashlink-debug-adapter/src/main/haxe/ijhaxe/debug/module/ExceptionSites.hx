package ijhaxe.debug.module;

import ijhaxe.debug.Pointer;
import format.hl.Data.Opcode;

/**
	A throw site: an `OThrow`/`ORethrow` opcode, its machine address and the
	register holding the thrown value.
**/
typedef ThrowSite = {address:Pointer, fidx:Int, op:Int, reg:Int};

/**
	Lists every `OThrow`/`ORethrow` opcode in the program with its machine
	address, so exception breakpoints can plant an INT3 at each one. It scans
	only the decoded opcodes, never the data sections that hold embedded
	assets, and computes the list once, on first use.
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

	// The register a throw or rethrow opcode throws, or -1 for any other opcode.
	static inline function throwRegister(op:Opcode):Int {
		return switch (op) {
			case OThrow(reg): reg;
			case ORethrow(reg): reg;
			default: -1;
		}
	}
}
