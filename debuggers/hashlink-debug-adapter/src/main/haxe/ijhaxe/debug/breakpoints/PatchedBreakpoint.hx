package ijhaxe.debug.breakpoints;

import ijhaxe.debug.Pointer;

/**
	A breakpoint installed in the debuggee: the machine address patched with an
	INT3, the original byte to restore, and the source position it maps to.
**/
typedef PatchedBreakpoint = {
	var id:Int;
	var address:Pointer;
	var originalByte:Int;

	var fidx:Int;
	var op:Int;
	var file:String;
	var line:Int;

	// An optional Haxe expression evaluated at each hit; the debuggee stops only
	// when it is true. Null or empty for an unconditional breakpoint.
	var condition:Null<String>;
}
