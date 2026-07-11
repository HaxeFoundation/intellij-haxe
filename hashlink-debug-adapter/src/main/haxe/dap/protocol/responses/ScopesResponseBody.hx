package dap.protocol.responses;
import dap.protocol.Scope;

/**
 * Body of the "scopes" response.
 */
typedef ScopesResponseBody = {
	var scopes:Array<Scope>;
}
