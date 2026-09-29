package ijhaxe.debug.layout;

import format.hl.Data.HLType;

/**
	The byte offset of an object field or enum parameter within its value, plus its type.
**/
typedef FieldLayout = {
	var name:String;
	var offset:Int;
	var type:HLType;
}
