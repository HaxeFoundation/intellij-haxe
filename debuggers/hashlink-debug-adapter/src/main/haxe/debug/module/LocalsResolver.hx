package debug.module;

import debug.module.LocalScopes.LocalAssign;

/**
	Lists the named locals and arguments visible at a given opcode, resolving each
	to the bytecode register it occupies, from the function's `assigns` debug table.

	Encoding (verified against compiled fixtures):
	 - Arguments have `position < 0`. The named-argument assigns, in table order, map
	   to the argument registers starting at `argCount - namedArgs` (so an instance
	   method's unnamed `this` occupies register 0 and the named args follow).
	 - Locals have `position >= 0`; the register is the destination of the opcode at
	   that position. Which register a NAME means at the current opcode is scope
	   dependent (shadowing, loops, register reuse) and resolved through the
	   control-flow graph by `LocalScopes` — one entry per visible name.
**/
class LocalsResolver {
	final module:ModuleDebugInfo;
	final scopeCache:Map<Int, LocalScopes> = new Map();

	public function new(module:ModuleDebugInfo) {
		this.module = module;
	}

	public function localsAt(fidx:Int, currentOp:Int):Array<LocalVar> {
		var assigns = module.assignsOf(fidx);

		// arguments: the named ones map, in order, onto the trailing argument registers
		var namedArgs = [for (a in assigns) if (a.position < 0) a];
		var argStart = module.argCount(fidx) - namedArgs.length;
		var arguments:Array<LocalVar> = [];
		// an unnamed leading argument is the receiver: an instance method's `this`
		// (or a closure's captured environment, which HL passes the same way)
		if (argStart >= 1) {
			arguments.push({name: "this", register: 0});
		}
		for (i in 0...namedArgs.length) {
			arguments.push({name: module.stringAt(namedArgs[i].varName), register: argStart + i});
		}

		var locals = scopesOf(fidx).visibleLocals(currentOp);

		// a local shadowing an argument name wins while it is in scope
		var localNames = [for (l in locals) l.name => true];
		var result = [for (a in arguments) if (!localNames.exists(a.name)) a];
		for (l in locals) {
			result.push(l);
		}
		return result;
	}

	function scopesOf(fidx:Int):LocalScopes {
		var cached = scopeCache.get(fidx);
		if (cached != null) {
			return cached;
		}
		var locals:Array<LocalAssign> = [
			for (a in module.assignsOf(fidx))
				if (a.position >= 0) {name: module.stringAt(a.varName), position: a.position}
		];
		var scopes = new LocalScopes(new CodeGraph(module.opcodes(fidx)), locals, pos -> module.dstRegister(fidx, pos));
		scopeCache.set(fidx, scopes);
		return scopes;
	}
}
