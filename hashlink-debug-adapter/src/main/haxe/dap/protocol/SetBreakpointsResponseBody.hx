package dap.protocol;

/**
 * Body of the "setBreakpoints" response.
 */
typedef SetBreakpointsResponseBody = {
	var breakpoints:Array<Breakpoint>;
}
