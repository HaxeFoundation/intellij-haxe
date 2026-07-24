package ijhaxe.dap.protocol.responses;
import ijhaxe.dap.protocol.Variable;

/**
	Body of the "variables" response.
**/
typedef VariablesResponseBody = {
	var variables:Array<Variable>;
}
