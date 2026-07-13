package dap.protocol.requests;

/**
 * Arguments for the "setExceptionBreakpoints" request: the ids of the exception
 * filters (from Capabilities.exceptionBreakpointFilters) the client wants active.
 * A non-empty list containing "all" enables breaking on every thrown exception.
 */
typedef SetExceptionBreakpointsArguments = {
	var filters:Array<String>;
}
