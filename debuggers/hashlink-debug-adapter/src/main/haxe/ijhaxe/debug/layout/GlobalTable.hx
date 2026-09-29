package ijhaxe.debug.layout;

import format.hl.Data.HLType;

/**
	Computes the byte offset of each global within the runtime's global data
	block, the way `hl_module_init` fills `globals_indexes`: in index order,
	each global aligned to its own size. The slot of a class's statics global
	holds a pointer to the class's statics singleton.
**/
class GlobalTable {
	final align:Align;
	final globals:Array<HLType>;

	public function new(align:Align, globals:Array<HLType>) {
		this.align = align;
		this.globals = globals;
	}

	/**
		Byte offset of global `index` within the global data block.
	**/
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
