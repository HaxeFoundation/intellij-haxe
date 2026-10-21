package ijhaxe.debug.session;

/**
	The resolved arguments of a launch request.

	`hlPath` is the HashLink executable; it defaults to the VM running the
	adapter. When `attachPid` is set, the client spawned the debuggee with
	`--debug <debugPort> --debug-wait` and owns its stdio and lifetime; the
	adapter attaches instead of spawning.

	TODO: honour `stopOnEntry`; it is accepted and ignored.
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
