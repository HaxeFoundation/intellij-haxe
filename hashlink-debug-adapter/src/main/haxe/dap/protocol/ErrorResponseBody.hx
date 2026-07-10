package dap.protocol;

/**
 * Body of an error response (success = false).
 */
typedef ErrorResponseBody = {
	@:optional var error:Message;
}
