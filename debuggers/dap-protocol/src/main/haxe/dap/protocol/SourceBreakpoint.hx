package dap.protocol;

/**
	A breakpoint location as requested by the client in "setBreakpoints".
**/
typedef SourceBreakpoint = {
	var line:Int;
	var ?column:Int;
	var ?condition:String;
	var ?hitCondition:String;
	var ?logMessage:String;
}
