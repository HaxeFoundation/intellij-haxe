package ijhaxe.dap.protocol.requests;

/**
	Arguments for the "stepIn" request.
**/
typedef StepInArguments = {
	var threadId:Int;
	var ?singleThread:Bool;
	var ?targetId:Int;
	var ?granularity:String;
}
