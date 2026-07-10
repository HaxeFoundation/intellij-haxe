package dap.protocol;

/**
 * Body of the "stackTrace" response.
 */
typedef StackTraceResponseBody = {
	var stackFrames:Array<StackFrame>;
	@:optional var totalFrames:Int;
}
