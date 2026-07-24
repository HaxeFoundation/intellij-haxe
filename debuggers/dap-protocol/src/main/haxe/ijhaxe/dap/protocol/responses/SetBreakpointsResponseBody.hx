package ijhaxe.dap.protocol.responses;
import ijhaxe.dap.protocol.Breakpoint;

/**
	Body of the "setBreakpoints" response.
**/
typedef SetBreakpointsResponseBody = {
	var breakpoints:Array<Breakpoint>;
}
