package dap.protocol.responses;

/**
 * Body of the "continue" response.
 */
typedef ContinueResponseBody = {
	@:optional var allThreadsContinued:Bool;
}
