package debug.layout;

import format.hl.Data.HLType;

/**
	Where a bytecode register lives relative to the frame base (`ebp`): a signed
	byte offset (negative = locals/spilled args below ebp, positive = stack-passed
	args above ebp) plus the register's type.
**/
typedef RegisterSlot = {
	var t:HLType;
	var offset:Int;
}
