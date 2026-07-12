package dap.protocol.requests;

/**
 * Arguments for the "launch" request (adapter-specific per the DAP spec).
 * `program` is the path to a .hl file compiled with -debug.
 * `hlPath` overrides the HashLink executable used to run it; when absent
 * the adapter uses the VM it is itself running on.
 * `stopOnEntry` is accepted but not yet honored (deferred to the stepping milestone).
 *
 * Attach mode: when `attachPid` is present the client has already spawned
 * `hl --debug <debugPort> --debug-wait <program>` itself and the adapter only
 * attaches to that pid (and reads the handshake from `debugPort`). The client
 * then owns the debuggee's stdio and lifetime. This exists because spawning
 * the debuggee from the adapter (an HL process) forces SW_HIDE onto the
 * debuggee's first window on Windows — see docs/README.md.
 */
typedef LaunchRequestArguments = {
	var program:String;
	@:optional var args:Array<String>;
	@:optional var cwd:String;
	@:optional var hlPath:String;
	@:optional var stopOnEntry:Bool;
	@:optional var attachPid:Null<Int>;
	@:optional var debugPort:Null<Int>;
}
