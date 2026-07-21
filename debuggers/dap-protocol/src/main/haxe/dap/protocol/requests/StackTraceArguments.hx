package dap.protocol.requests;

/**
	Arguments for the "stackTrace" request.
**/
typedef StackTraceArguments = {
	var threadId:Int;
	var ?startFrame:Int;
	var ?levels:Int;
}
