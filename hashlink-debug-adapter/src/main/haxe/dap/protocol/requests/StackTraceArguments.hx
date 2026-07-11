package dap.protocol.requests;

/**
 * Arguments for the "stackTrace" request.
 */
typedef StackTraceArguments = {
	var threadId:Int;
	@:optional var startFrame:Int;
	@:optional var levels:Int;
}
