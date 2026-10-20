package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;

import format.hl.Data.HLType;

/**
	A slot in debuggee memory: its address and the static type stored there,
	such as the location of an object field or array element. A WriteTarget
	adds the slot's name.
**/
typedef AddressedValue = {address:Pointer, type:HLType}
