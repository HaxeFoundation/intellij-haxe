package dap.protocol.requests;

/**
 * Arguments for the "stepOut" request.
 */
typedef StepOutArguments = {
	var threadId:Int;
	@:optional var singleThread:Bool;
	@:optional var granularity:String;
}
