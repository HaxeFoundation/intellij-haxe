package dap.protocol;

/**
 * One variable in a "variables" response. `variablesReference` > 0 marks an
 * expandable value (object/array); 0 means a leaf.
 */
typedef Variable = {
	var name:String;
	var value:String;
	var variablesReference:Int;
	@:optional var type:String;
	@:optional var namedVariables:Int;
	@:optional var indexedVariables:Int;
}
