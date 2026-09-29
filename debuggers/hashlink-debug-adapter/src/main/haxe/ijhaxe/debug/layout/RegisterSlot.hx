package ijhaxe.debug.layout;

import format.hl.Data.HLType;

/**
	Where a bytecode register lives, as a signed byte offset from the frame base
	(`ebp`), plus the register's type. Locals and spilled arguments have
	negative offsets; stack-passed arguments have positive ones.
**/
typedef RegisterSlot = {
	var t:HLType;
	var offset:Int;
}
