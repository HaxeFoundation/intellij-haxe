package dap.protocol.responses;
import dap.protocol.StackFrame;

/**
 * Body of the "stackTrace" response.
 */
typedef StackTraceResponseBody = {
	var stackFrames:Array<StackFrame>;
	var ?totalFrames:Int;
}
