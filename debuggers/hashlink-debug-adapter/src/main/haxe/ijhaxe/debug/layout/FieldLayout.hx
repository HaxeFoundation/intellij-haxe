package ijhaxe.debug.layout;

import format.hl.Data.HLType;

/**
	One object field's byte offset within an instance, plus its type.
**/
typedef FieldLayout = {
	var name:String;
	var offset:Int;
	var type:HLType;
}
