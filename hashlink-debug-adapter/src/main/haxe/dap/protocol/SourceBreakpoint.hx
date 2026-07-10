package dap.protocol;

/**
 * A breakpoint location as requested by the client in "setBreakpoints".
 */
typedef SourceBreakpoint = {
	var line:Int;
	@:optional var column:Int;
	@:optional var condition:String;
	@:optional var hitCondition:String;
	@:optional var logMessage:String;
}
