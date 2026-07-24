package ijhaxe.dap.protocol;

/**
	The capabilities the adapter reports in the "initialize" response body.
**/
typedef Capabilities = {
	var ?supportsConfigurationDoneRequest:Bool;
	var ?supportsVariableType:Bool;
	var ?supportsEvaluateForHovers:Bool;
	var ?supportsSetVariable:Bool;
	// We evaluate a breakpoint's `condition` at each hit.
	var ?supportsConditionalBreakpoints:Bool;
	// stepInTargets lists the calls on the stopped line; stepIn takes a targetId.
	var ?supportsStepInTargetsRequest:Bool;
	// The exception categories the client can toggle via setExceptionBreakpoints.
	var ?exceptionBreakpointFilters:Array<ExceptionBreakpointsFilter>;
}

/**
	One toggle in the client's exception-breakpoint list (only the required fields).
**/
typedef ExceptionBreakpointsFilter = {
	var filter:String;
	var label:String;
}
