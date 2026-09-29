package ijhaxe.debug.module;

/**
	Resolves which register a local variable NAME occupies at a given opcode,
	respecting source scopes (a port of hld `CodeGraph.getLocal`/`lookupLocal`).

	The `.hl` debug tables carry no scope information. They only record
	`assigns` entries ("at opcode N this name was written to the register that
	N writes"), and registers are reused for temporaries once a source scope
	ends. The binding of a name at an opcode is therefore reconstructed from
	the control flow, over basic blocks (runs of opcodes without branches):
	 - Within the current block, the last assignment of the name BEFORE the
	   opcode wins, so a `for`-loop `x` shadows an outer `x` inside the loop.
	 - Otherwise the lookup continues in the predecessor blocks, skipping loop
	   back-edges (predecessors that start later). That skip is what brings
	   back the OUTER binding after a shadowing loop ends.
	 - If the incoming branches disagree on the register (a name assigned in
	   only one arm of an `if`), the name is out of scope.
**/
class LocalScopes {
	final assigns:Array<LocalAssign>; // ordered by position, positions >= 0 only
	final destinationOf:Int->Int;
	final blocks:Array<ScopeBlock> = [];
	final blockStarts:Array<Int> = [];
	// incremented per query, so a block can tell whether it was already visited in this one
	var currentTag:Int = 0;

	public function new(graph:CodeGraph, assigns:Array<LocalAssign>, destinationOf:Int->Int) {
		this.assigns = assigns;
		this.destinationOf = destinationOf;
		var count = graph.opCount();
		if (count == 0) {
			return;
		}

		// a block starts at the entry, at every jump target and after every branch
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
				blockAt(target).predecessors.push(block);
			}
		}
		for (a in assigns) {
			if (a.position < 0) {
				continue;
			}
			var block = blockAt(a.position);
			var positions = block.assignPositions.get(a.name);
			if (positions == null) {
				positions = [];
				block.assignPositions.set(a.name, positions);
			}
			positions.push(a.position); // assigns are ordered by position, so this stays ascending
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
		The locals in scope at opcode `pos`, one entry per name, each with its
		current register. An inner binding replaces a shadowed outer one.
	**/
	public function visibleLocals(pos:Int):Array<LocalVar> {
		var seen = new Map<String, Bool>();
		var result:Array<LocalVar> = [];
		for (a in assigns) {
			if (a.position > pos) {
				break; // the table is ordered by position
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
			return block.visitResult; // memoized for this query; without back-edges the walk is acyclic
		}
		block.visitTag = currentTag;
		block.visitResult = -1;
		var positions = block.assignPositions.get(name);
		if (positions != null) {
			var last = -1;
			for (p in positions) {
				if (p >= pos) {
					break;
				}
				last = p; // strictly before pos: the assign op itself has not written the value yet
			}
			if (last >= 0) {
				return block.visitResult = destinationOf(last);
			}
		}
		var found = NONE;
		for (predecessor in block.predecessors) {
			if (predecessor.start >= block.start) {
				continue; // a loop back-edge: the loop body cannot provide the binding from before the loop
			}
			var resolved = lookup(predecessor, name, pos);
			if (found == NONE) {
				found = resolved;
			} else if (resolved != found) {
				return block.visitResult = -1; // the branches disagree: out of scope
			}
		}
		return block.visitResult = found == NONE ? -1 : found;
	}

	function blockAt(pos:Int):ScopeBlock {
		// the greatest block start <= pos (binary search over the sorted starts)
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
	public final predecessors:Array<ScopeBlock> = [];
	// variable name -> the positions in this block where it is assigned, ascending
	public final assignPositions:Map<String, Array<Int>> = new Map();
	public var visitTag:Int = 0;
	public var visitResult:Int = -1;

	public function new(start:Int, end:Int) {
		this.start = start;
		this.end = end;
	}
}
