package ijhaxe.dap.protocol.requests;

/**
	Arguments for the "stepOut" request.
**/
typedef StepOutArguments = {
	var threadId:Int;
	var ?singleThread:Bool;
	var ?granularity:String;
}
