package ijhaxe.debug.module;

/**
	A local variable or argument visible at an opcode: its source name and the
	register it occupies there.
**/
typedef LocalVar = {
	var name:String;
	var register:Int;
}
