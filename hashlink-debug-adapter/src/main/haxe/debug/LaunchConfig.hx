package debug;

/**
 * Resolved arguments for launching a debuggee.
 * `hlPath` is the HashLink executable (defaults to the adapter's own VM).
 * `stopOnEntry` is accepted but not yet honored.
 */
typedef LaunchConfig = {
	var program:String;
	var args:Array<String>;
	var cwd:Null<String>;
	var hlPath:String;
	var stopOnEntry:Bool;
}
