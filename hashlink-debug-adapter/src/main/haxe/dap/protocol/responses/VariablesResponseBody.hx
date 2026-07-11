package dap.protocol.responses;
import dap.protocol.Variable;

/**
 * Body of the "variables" response.
 */
typedef VariablesResponseBody = {
	var variables:Array<Variable>;
}
