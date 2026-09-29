package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;

/**
	One map entry: the key as display text and the address of the value,
	which is read as Dynamic.
**/
typedef MapEntrySlot = {
	var key:String;
	var valueAddress:Pointer;
}
