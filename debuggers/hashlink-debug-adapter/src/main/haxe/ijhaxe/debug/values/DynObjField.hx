package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;
import format.hl.Data.HLType;

/**
	One field of a dynamic object (vdynobj): its name, the address of its value
	and its runtime type.
**/
typedef DynObjField = {
	var name:String;
	var address:Pointer;
	var type:HLType;
}
