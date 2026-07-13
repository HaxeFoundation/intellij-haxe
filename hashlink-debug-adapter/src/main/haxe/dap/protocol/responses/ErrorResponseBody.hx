package dap.protocol.responses;

/**
 * Body of an error response (success = false).
 */
typedef ErrorResponseBody = {
	var ?error:Message;
}
