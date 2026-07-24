package ijhaxe.debug.breakpoints;

import ijhaxe.debug.Pointer;

/**
	A breakpoint physically installed in the debuggee: the INT3-patched machine
	address, the original byte we must restore, and the source location it maps to.
**/
typedef PatchedBreakpoint = {
	var id:Int;
	var address:Pointer;
	var originalByte:Int;
	var fidx:Int;
	var op:Int;
	var file:String;
	var line:Int;
	// Optional Haxe expression evaluated at each hit; the debuggee stops only
	// when it is true. Null/empty = an unconditional breakpoint.
	var condition:Null<String>;
}
