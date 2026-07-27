package ijhaxe.debug.breakpoints;

/**
	The adapter's verdict on a requested breakpoint, echoed back to the client.
**/
typedef BreakpointResult = {
	var id:Int;
	var verified:Bool;
	var line:Int;
	var ?message:String;
	var ?sourcePath:String;
}
