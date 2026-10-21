package ijhaxe.debug.breakpoints;

import ijhaxe.debug.Pointer;

/**
	A breakpoint to install: its client id, the address to patch, the code
	position (fidx/op), the source position (file/line) and an optional
	condition. BreakpointPlanner resolves these from a requested source line,
	and Breakpoints.setForSource installs them.
**/
typedef BreakpointLocation = {id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int, condition:Null<String>}
