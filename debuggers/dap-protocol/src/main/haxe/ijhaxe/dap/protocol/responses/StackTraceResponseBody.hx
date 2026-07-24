package ijhaxe.dap.protocol.responses;
import ijhaxe.dap.protocol.StackFrame;

/**
	Body of the "stackTrace" response.
**/
typedef StackTraceResponseBody = {
	var stackFrames:Array<StackFrame>;
	var ?totalFrames:Int;
}
