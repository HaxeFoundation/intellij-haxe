package ijhaxe.debug.values;

/**
	A named, decoded value for the client: a variable, field, element or entry.
**/
typedef VariableInfo = {
	var name:String;
	var value:String;
	var type:String;
	var reference:Int;
	// The client picks the icon from it; absent for elements and entries,
	// which need no special icon.
	var ?kind:VariableKind;
}
