package ijhaxe.debug.module;

/**
	Resolves which register a local variable NAME occupies at a given opcode,
	honouring source scopes (a port of hld `CodeGraph.getLocal`/`lookupLocal`).

	The `.hl` debug tables carry no scope information — only append-only
	`assigns` entries ("at opcode N this name was written to `dst(N)`"), and
	registers are freely reused for temporaries once a source scope ends. So the
	binding of a name at an opcode must be reconstructed from control flow:

	 - within the current basic block, the last assignment of that name BEFORE
	   the opcode wins (a `for`-loop `x` shadows an outer `x` inside the loop);
	 - otherwise the lookup recurses into predecessor blocks, skipping loop
	   back-edges (predecessors that start later), which is what makes the name
	   fall back to the OUTER binding after a shadowing loop ends;
	 - if the incoming branches disagree on the register (a name assigned in
	   only one arm of an `if`), the name is out of scope and dropped.

	Pure; built from a CodeGraph plus the function's local assigns, unit-tested
	with synthetic opcodes.
**/
class LocalScopes {
	final assigns:Array<LocalAssign>; // position-ordered, positions >= 0 only
	final dstOf:Int->Int;
	final blocks:Array<ScopeBlock> = [];
	final blockStarts:Array<Int> = [];
	var currentTag:Int = 0;

	public function new(graph:CodeGraph, assigns:Array<LocalAssign>, dstOf:Int->Int) {
		this.assigns = assigns;
		this.dstOf = dstOf;
		var count = graph.opCount();
		if (count == 0) {
			return;
		}

		// block leaders: entry, every jump target, and every op after a branch
		var leaders = new Map<Int, Bool>();
		leaders.set(0, true);
		for (op in 0...count) {
			var successors = graph.successors(op);
			if (successors.length == 1 && successors[0] == op + 1) {
				continue;
			}
			for (target in successors) {
				leaders.set(target, true);
			}
			if (op + 1 < count) {
				leaders.set(op + 1, true);
			}
		}
		for (start in leaders.keys()) {
			blockStarts.push(start);
		}
		blockStarts.sort((a, b) -> a - b);
		for (i in 0...blockStarts.length) {
			var end = (i + 1 < blockStarts.length ? blockStarts[i + 1] : count) - 1;
			blocks.push(new ScopeBlock(blockStarts[i], end));
		}
		for (block in blocks) {
			for (target in graph.successors(block.end)) {
				blockAt(target).prev.push(block);
			}
		}
		for (a in assigns) {
			if (a.position < 0) {
				continue;
			}
			var block = blockAt(a.position);
			var writes = block.written.get(a.name);
			if (writes == null) {
				writes = [];
				block.written.set(a.name, writes);
			}
			writes.push(a.position); // assigns are position-ordered, so writes stay ascending
		}
	}

	/**
		The register holding `name` at opcode `pos`, or -1 when out of scope.
	**/
	public function registerOf(name:String, pos:Int):Int {
		if (blocks.length == 0) {
			return -1;
		}
		currentTag++;
		return lookup(blockAt(pos), name, pos);
	}

	/**
		The locals in scope at opcode `pos`, one entry per NAME (a shadowing
		binding replaces the outer one), each resolved to its current register.
	**/
	public function visibleLocals(pos:Int):Array<LocalVar> {
		var seen = new Map<String, Bool>();
		var result:Array<LocalVar> = [];
		for (a in assigns) {
			if (a.position > pos) {
				break; // the table is position-ordered
			}
			if (seen.exists(a.name)) {
				continue;
			}
			seen.set(a.name, true);
			var register = registerOf(a.name, pos);
			if (register >= 0) {
				result.push({name: a.name, register: register});
			}
		}
		return result;
	}

	static inline var NONE = -2; // no predecessor resolved the name yet

	function lookup(block:ScopeBlock, name:String, pos:Int):Int {
		if (block.visitTag == currentTag) {
			return block.visitResult; // memoized for this query (the walk is a DAG)
		}
		block.visitTag = currentTag;
		block.visitResult = -1;
		var writes = block.written.get(name);
		if (writes != null) {
			var last = -1;
			for (p in writes) {
				if (p >= pos) {
					break;
				}
				last = p; // strictly before pos: at the assign op itself the value isn't written yet
			}
			if (last >= 0) {
				return block.visitResult = dstOf(last);
			}
		}
		var found = NONE;
		for (predecessor in block.prev) {
			if (predecessor.start >= block.start) {
				continue; // a loop back-edge: the body can't provide the pre-loop binding
			}
			var resolved = lookup(predecessor, name, pos);
			if (found == NONE) {
				found = resolved;
			} else if (resolved != found) {
				return block.visitResult = -1; // branches disagree: out of scope
			}
		}
		return block.visitResult = found == NONE ? -1 : found;
	}

	function blockAt(pos:Int):ScopeBlock {
		// greatest block start <= pos (binary search over the sorted starts)
		var lo = 0;
		var hi = blockStarts.length - 1;
		while (lo < hi) {
			var mid = (lo + hi + 1) >> 1;
			if (blockStarts[mid] <= pos) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return blocks[lo];
	}
}

typedef LocalAssign = {name:String, position:Int}

private class ScopeBlock {
	public final start:Int;
	public final end:Int;
	public final prev:Array<ScopeBlock> = [];
	public final written:Map<String, Array<Int>> = new Map();
	public var visitTag:Int = 0;
	public var visitResult:Int = -1;

	public function new(start:Int, end:Int) {
		this.start = start;
		this.end = end;
	}
}
