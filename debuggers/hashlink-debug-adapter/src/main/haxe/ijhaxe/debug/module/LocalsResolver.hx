package ijhaxe.debug.module;

import ijhaxe.debug.module.LocalScopes.LocalAssign;

/**
	Lists the named locals and arguments visible at an opcode, each with the
	bytecode register it occupies, from the function's `assigns` debug table.

	Arguments have `position < 0`. Their assigns, in table order, name the
	LAST argument registers, so an unnamed leading argument (an instance
	method's `this`) keeps register 0 and the named arguments follow.

	Locals have `position >= 0`, and their register is the one written by the
	opcode at that position. Which register a name means at a given opcode
	depends on scope (shadowing, loops, register reuse), so `LocalScopes`
	resolves it through the control-flow graph.
**/
class LocalsResolver {
	final module:ModuleDebugInfo;
	final scopeCache:Map<Int, LocalScopes> = new Map();

	public function new(module:ModuleDebugInfo) {
		this.module = module;
	}

	public function localsAt(fidx:Int, currentOp:Int):Array<LocalVar> {
		var assigns = module.assignsOf(fidx);

		var namedArgs = [for (a in assigns) if (a.position < 0) a];
		var argStart = module.argCount(fidx) - namedArgs.length;
		var arguments:Array<LocalVar> = [];
		// an unnamed leading argument is an instance method's `this`, or the
		// captured environment of a closure, which HL passes the same way
		if (argStart >= 1) {
			arguments.push({name: "this", register: 0});
		}
		for (i in 0...namedArgs.length) {
			arguments.push({name: module.stringAt(namedArgs[i].varName), register: argStart + i});
		}

		var locals = scopesOf(fidx).visibleLocals(currentOp);

		// a local in scope hides an argument of the same name
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
		var scopes = new LocalScopes(new CodeGraph(module.opcodes(fidx)), locals, pos -> module.destinationRegister(fidx, pos));
		scopeCache.set(fidx, scopes);
		return scopes;
	}
}
