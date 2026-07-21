package debug.values;

import debug.Pointer;
import format.hl.Data.HLType;

/**
	One field of a runtime dynamic object (vdynobj): resolved name, the address
	of its value slot, and its runtime type.
**/
typedef DynObjField = {
	var name:String;
	var address:Pointer;
	var type:HLType;
}
