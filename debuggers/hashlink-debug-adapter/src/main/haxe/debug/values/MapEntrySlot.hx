package debug.values;

import debug.Pointer;

/**
	One live entry of a native HashLink map: a display string for the key and
	the address of the value slot (a dynamic pointer, read as HDyn).
**/
typedef MapEntrySlot = {
	var key:String;
	var valueAddress:Pointer;
}
