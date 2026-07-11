package debug;

/**
 * The adapter's verdict on a requested breakpoint, echoed back to the client.
 */
typedef BreakpointResult = {
	var id:Int;
	var verified:Bool;
	var line:Int;
	@:optional var message:String;
	@:optional var sourcePath:String;
}
