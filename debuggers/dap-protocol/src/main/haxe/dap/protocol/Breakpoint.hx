package dap.protocol;

/**
	The adapter's view of a breakpoint, returned in the "setBreakpoints" response.
**/
typedef Breakpoint = {
	var verified:Bool;
	var ?id:Int;
	var ?message:String;
	var ?source:Source;
	var ?line:Int;
	var ?column:Int;
}
