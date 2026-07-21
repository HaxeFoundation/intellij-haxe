package dap.protocol.requests;

/**
	Arguments for the "next" (step over) request.
**/
typedef NextArguments = {
	var threadId:Int;
	var ?singleThread:Bool;
	var ?granularity:String;
}
