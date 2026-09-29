package ijhaxe.debug.values;
import ijhaxe.dap.protocol.Variable;

/**
	A decoded value ready for a DAP `Variable`: a display string, a type label
	and a `reference`. A reference of 0 marks a leaf; a positive reference lets
	the client request the value's children.
**/
typedef DecodedValue = {
	var value:String;
	var type:String;
	var reference:Int;
}
