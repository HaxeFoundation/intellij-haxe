package ijhaxe.dap.protocol.requests;

/**
	Arguments for the HashLink adapter's "launch" request; the DAP spec leaves
	their shape to each adapter. `program` is the path to the .hl file.
	`hlPath` overrides the HashLink executable that runs it; by default the
	adapter uses the VM it runs on itself. `stopOnEntry` is accepted but
	ignored.

	Attach mode: with `attachPid`, the client has already spawned
	`hl --debug <debugPort> --debug-wait <program>`, and the adapter only
	attaches to that pid and reads the handshake from `debugPort`. The client
	then owns the debuggee's stdio and lifetime. Attach mode exists because a
	debuggee spawned by the adapter, itself an HL process, gets its first
	window hidden on Windows (see the adapter's docs/README.md).
**/
typedef LaunchRequestArguments = {
	var program:String;
	var ?args:Array<String>;
	var ?cwd:String;

	var ?hlPath:String;
	var ?stopOnEntry:Bool;

	var ?attachPid:Null<Int>;
	var ?debugPort:Null<Int>;
}
