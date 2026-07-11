package dap.protocol.responses;
import dap.protocol.Breakpoint;

/**
 * Body of the "setBreakpoints" response.
 */
typedef SetBreakpointsResponseBody = {
	var breakpoints:Array<Breakpoint>;
}
