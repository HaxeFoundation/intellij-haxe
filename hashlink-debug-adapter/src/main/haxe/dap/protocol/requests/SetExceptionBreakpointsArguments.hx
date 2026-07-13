package dap.protocol.requests;

/**
 * Arguments for the "setExceptionBreakpoints" request: the ids of the exception
 * filters (from Capabilities.exceptionBreakpointFilters) the client wants active.
 * A non-empty list containing "all" enables breaking on every thrown exception.
 *
 * `filterTypes` is our extension for per-class exception breakpoints: exception
 * class names (FQN or simple) to stop on regardless of the "all"/"uncaught"
 * filters — the debugger stops when a thrown value's class or a superclass matches.
 */
typedef SetExceptionBreakpointsArguments = {
	var filters:Array<String>;
	var ?filterTypes:Array<String>;
}
