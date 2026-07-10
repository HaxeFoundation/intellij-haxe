package dap.protocol;

/**
 * Body of the "continue" response.
 */
typedef ContinueResponseBody = {
	@:optional var allThreadsContinued:Bool;
}
