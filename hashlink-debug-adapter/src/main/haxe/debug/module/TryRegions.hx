package debug.module;

// One `try` protection range in a function: op `p` is protected (a catch is
// active) when `start < p <= end`. `start` is the OTrap's op, `end` is the last
// protected op (the catch handler is at `end+1`). Module-private.
private typedef Region = {start:Int, end:Int};

/**
 * Static `try` protection ranges per function, derived from `OTrap` opcodes.
 *
 * An `OTrap(_, end)` at op `i` opens a try whose catch handler is at `i+1+end`
 * (the same branch target CodeGraph uses), so the protected body is `i+1 .. i+end`
 * — a throw at op `p` is caught by that trap when `i < p <= i+end`. Used to decide
 * whether a throw will be caught: if any live frame's current op is inside a
 * protected range, a `catch` is active up the stack. Typed catches
 * (`catch(e:SpecificType)`) are approximated as always matching — any active try
 * counts as catching. Computed per function, cached.
 */
class TryRegions {
	final module:ModuleDebugInfo;
	final cache:Map<Int, Array<Region>> = new Map();

	public function new(module:ModuleDebugInfo) {
		this.module = module;
	}

	/** True when op `op` of function `fidx` is inside a `try` block. */
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
