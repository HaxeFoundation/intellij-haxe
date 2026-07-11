package dap.protocol;

/**
 * Arguments for the "variables" request.
 */
typedef VariablesArguments = {
	var variablesReference:Int;
	@:optional var filter:String; // "indexed" | "named"
	@:optional var start:Int;
	@:optional var count:Int;
}
