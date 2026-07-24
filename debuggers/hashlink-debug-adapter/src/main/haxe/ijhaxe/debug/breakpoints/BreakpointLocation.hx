package ijhaxe.debug.breakpoints;

import ijhaxe.debug.Pointer;

/**
	A breakpoint to install: its client id, the patch address, the code position
	(fidx/op), the source it maps to (file/line), and an optional hit condition.
	DebugSession resolves these from a source+line request and hands them to
	Breakpoints.setForSource.
**/
typedef BreakpointLocation = {id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int, condition:Null<String>}
