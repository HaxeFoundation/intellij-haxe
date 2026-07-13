package dap.protocol.events;

/**
 * Body of the "stopped" event.
 * `reason` is e.g. "breakpoint" or "exception".
 */
typedef StoppedEventBody = {
	var reason:String;
	var ?threadId:Int;
	var ?allThreadsStopped:Bool;
	var ?hitBreakpointIds:Array<Int>;
	var ?description:String;
}
