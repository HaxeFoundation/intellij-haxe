package dap.protocol.requests;

/**
 * Arguments for the "stepIn" request.
 */
typedef StepInArguments = {
	var threadId:Int;
	@:optional var singleThread:Bool;
	@:optional var targetId:Int;
	@:optional var granularity:String;
}
