package ijhaxe.debug.breakpoints;

/**
	A breakpoint the client asked for: the source line, the id the adapter
	assigned to it (ids are stable across re-verification), and an optional
	condition expression evaluated at each hit (the debuggee only stops when it
	is true).
**/
typedef RequestedBreakpoint = {
	var id:Int;
	var line:Int;
	var condition:Null<String>;
}
