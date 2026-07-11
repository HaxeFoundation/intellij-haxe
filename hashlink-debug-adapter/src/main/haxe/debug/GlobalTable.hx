package debug;

import format.hl.Data.HLType;

/**
 * Computes the byte offset of each global within the runtime's global data block,
 * replicating `hl_module_init`'s `globals_indexes`: globals are laid out in index
 * order, each aligned to its own size. A class's statics singleton lives at
 * `globalsPtr + offset(proto.globalValue)` (the slot holds a pointer to it).
 *
 * Pure; unit-tested.
 */
class GlobalTable {
	final align:Align;
	final globals:Array<HLType>;

	public function new(align:Align, globals:Array<HLType>) {
		this.align = align;
		this.globals = globals;
	}

	/** Byte offset of global `index` within the global data block. */
	public function offsetOf(index:Int):Int {
		var size = 0;
		for (i in 0...index + 1) {
			size += align.padSize(size, globals[i]);
			if (i == index) {
				return size;
			}
			size += align.typeSize(globals[i]);
		}
		return size;
	}
}
