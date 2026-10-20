package ijhaxe.dap.protocol;

/**
	One variable in a "variables" response. A `variablesReference` above 0
	marks an expandable value (an object or array); 0 marks a leaf.
**/
typedef Variable = {
	var name:String;
	var value:String;
	var variablesReference:Int;

	var ?type:String;
	var ?namedVariables:Int;
	var ?indexedVariables:Int;

	// The classification that picks the client's icon ("local", "argument", "field", ...).
	var ?kind:String;
}
