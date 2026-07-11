package dap.protocol;

/**
 * Arguments for the "stepOut" request.
 */
typedef StepOutArguments = {
	var threadId:Int;
	@:optional var singleThread:Bool;
	@:optional var granularity:String;
}
