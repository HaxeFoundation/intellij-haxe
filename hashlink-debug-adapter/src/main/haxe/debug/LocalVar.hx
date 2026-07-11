package debug;

/**
 * A named local/argument visible at some opcode: its source name and the
 * bytecode register it currently occupies.
 */
typedef LocalVar = {
	var name:String;
	var register:Int;
}
