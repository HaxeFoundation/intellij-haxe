package debug.values;

/**
	A resolved variable the session hands back: name plus its decoded value.
**/
typedef VariableInfo = {
	var name:String;
	var value:String;
	var type:String;
	var reference:Int;
	// A classification the client maps to an icon; absent for elements/entries
	// that need no special icon.
	var ?kind:VariableKind;
}
