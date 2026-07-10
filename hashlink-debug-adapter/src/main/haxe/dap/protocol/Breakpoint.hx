package dap.protocol;

/**
 * The adapter's view of a breakpoint, returned in the "setBreakpoints" response.
 */
typedef Breakpoint = {
	var verified:Bool;
	@:optional var id:Int;
	@:optional var message:String;
	@:optional var source:Source;
	@:optional var line:Int;
	@:optional var column:Int;
}
