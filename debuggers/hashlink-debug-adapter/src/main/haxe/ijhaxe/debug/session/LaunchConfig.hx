package ijhaxe.debug.session;

/**
	Resolved arguments for launching a debuggee.
	`hlPath` is the HashLink executable (defaults to the adapter's own VM).
	`stopOnEntry` is accepted but not yet honored.
	When `attachPid` is set the debuggee was spawned by the client with
	`--debug <debugPort> --debug-wait`; the adapter attaches instead of spawning
	(the client owns the process's stdio and lifetime).
**/
typedef LaunchConfig = {
	var program:String;
	var args:Array<String>;
	var cwd:Null<String>;
	var hlPath:String;
	var stopOnEntry:Bool;
	var attachPid:Null<Int>;
	var debugPort:Null<Int>;
}
