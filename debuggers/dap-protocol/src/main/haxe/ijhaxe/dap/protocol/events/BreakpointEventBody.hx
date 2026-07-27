package ijhaxe.dap.protocol.events;
import ijhaxe.dap.protocol.Breakpoint;

/**
	Body of the "breakpoint" event, used to re-verify breakpoints
	that were answered provisionally (e.g. set before launch).
**/
typedef BreakpointEventBody = {
	var reason:String;
	var breakpoint:Breakpoint;
}
