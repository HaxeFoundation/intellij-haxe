package ijhaxe.debug.module;

// The range of one `try` block: op `p` is inside it when `start < p <= end`.
// `start` is the OTrap op and the catch handler is at `end + 1`.
private typedef Region = {start:Int, end:Int};

/**
	The `try` blocks of each function, derived from its `OTrap` opcodes.

	An `OTrap(_, end)` at op `i` opens a `try` whose catch handler is at
	`i + 1 + end`, the same jump target CodeGraph computes. Its body is
	therefore ops `i + 1` to `i + end`. A throw will be caught when the current
	op of any frame on the stack lies inside such a body. Every `catch` counts
	as matching, even a typed `catch (e:SomeType)`.
**/
class TryRegions {
	final module:ModuleDebugInfo;
	final cache:Map<Int, Array<Region>> = new Map();

	public function new(module:ModuleDebugInfo) {
		this.module = module;
	}

	/**
		True when op `op` of function `fidx` is inside a `try` block.
	**/
	public function isProtected(fidx:Int, op:Int):Bool {
		for (region in regionsOf(fidx)) {
			if (op > region.start && op <= region.end) {
				return true;
			}
		}
		return false;
	}

	function regionsOf(fidx:Int):Array<Region> {
		var cached = cache.get(fidx);
		if (cached != null) {
			return cached;
		}
		var regions:Array<Region> = [];
		var ops = module.opcodes(fidx);
		for (i in 0...ops.length) {
			switch (ops[i]) {
				case OTrap(_, end):
					regions.push({start: i, end: i + end});
				default:
			}
		}
		cache.set(fidx, regions);
		return regions;
	}
}
