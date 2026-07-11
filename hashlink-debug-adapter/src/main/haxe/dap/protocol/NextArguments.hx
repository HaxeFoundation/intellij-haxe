package dap.protocol;

/**
 * Arguments for the "next" (step over) request.
 */
typedef NextArguments = {
	var threadId:Int;
	@:optional var singleThread:Bool;
	@:optional var granularity:String;
}
