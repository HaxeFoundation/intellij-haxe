package dap.protocol;

/**
 * Body of the "stopped" event.
 * `reason` is e.g. "breakpoint" or "exception".
 */
typedef StoppedEventBody = {
	var reason:String;
	@:optional var threadId:Int;
	@:optional var allThreadsStopped:Bool;
	@:optional var hitBreakpointIds:Array<Int>;
	@:optional var description:String;
}
