package ijhaxe.dap.protocol.responses;
import ijhaxe.dap.protocol.Scope;

/**
	Body of the "scopes" response.
**/
typedef ScopesResponseBody = {
	var scopes:Array<Scope>;
}
