package dap.protocol;

/**
 * One variable in a "variables" response. `variablesReference` > 0 marks an
 * expandable value (object/array); 0 means a leaf.
 */
typedef Variable = {
	var name:String;
	var value:String;
	var variablesReference:Int;
	var ?type:String;
	var ?namedVariables:Int;
	var ?indexedVariables:Int;
	// Our classification for the client's icon (the wire form of debug.values.VariableKind).
	var ?kind:String;
}
