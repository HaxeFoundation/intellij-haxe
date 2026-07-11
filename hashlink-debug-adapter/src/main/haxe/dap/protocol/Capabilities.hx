package dap.protocol;

/**
 * The capabilities the adapter reports in the "initialize" response body.
 */
typedef Capabilities = {
	@:optional var supportsConfigurationDoneRequest:Bool;
	@:optional var supportsVariableType:Bool;
	@:optional var supportsEvaluateForHovers:Bool;
}
