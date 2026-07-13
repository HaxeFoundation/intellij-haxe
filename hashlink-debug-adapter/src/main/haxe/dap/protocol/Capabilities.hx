package dap.protocol;

/**
 * The capabilities the adapter reports in the "initialize" response body.
 */
typedef Capabilities = {
	var ?supportsConfigurationDoneRequest:Bool;
	var ?supportsVariableType:Bool;
	var ?supportsEvaluateForHovers:Bool;
	var ?supportsSetVariable:Bool;
}
