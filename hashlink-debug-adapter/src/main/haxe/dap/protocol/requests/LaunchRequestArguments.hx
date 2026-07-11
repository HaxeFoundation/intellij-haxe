package dap.protocol.requests;

/**
 * Arguments for the "launch" request (adapter-specific per the DAP spec).
 * `program` is the path to a .hl file compiled with -debug.
 * `hlPath` overrides the HashLink executable used to run it; when absent
 * the adapter uses the VM it is itself running on.
 * `stopOnEntry` is accepted but not yet honored (deferred to the stepping milestone).
 */
typedef LaunchRequestArguments = {
	var program:String;
	@:optional var args:Array<String>;
	@:optional var cwd:String;
	@:optional var hlPath:String;
	@:optional var stopOnEntry:Bool;
}
