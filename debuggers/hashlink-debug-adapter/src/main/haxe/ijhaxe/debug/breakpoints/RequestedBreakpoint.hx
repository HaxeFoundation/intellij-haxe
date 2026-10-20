package ijhaxe.debug.breakpoints;

/**
	A breakpoint the client asked for: the id the adapter assigned to it (it
	stays the same across re-verification), the source line, and an optional
	condition evaluated at each hit; the debuggee stops only when it is true.
**/
typedef RequestedBreakpoint = {
	var id:Int;
	var line:Int;
	var condition:Null<String>;
}
