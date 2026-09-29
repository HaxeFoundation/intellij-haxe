package ijhaxe.dap.protocol.requests;

/**
	Arguments for the "setExceptionBreakpoints" request: `filters` holds the ids
	of the exception filters the client wants active (see
	Capabilities.exceptionBreakpointFilters); "all" breaks on every thrown
	exception.

	`filterTypes` is an extension to DAP for per-class exception breakpoints:
	the full or simple names of exception classes to stop on, whatever the other
	filters say. The debugger stops when the class of a thrown value, or one of
	its superclasses, matches.
**/
typedef SetExceptionBreakpointsArguments = {
	var filters:Array<String>;
	var ?filterTypes:Array<String>;
}
