package dap.protocol.responses;

/**
 * Body of the "evaluate" response: the rendered value, its type label, and a
 * variablesReference when the result is expandable (0 = leaf).
 */
typedef EvaluateResponseBody = {
	var result:String;
	var ?type:String;
	var variablesReference:Int;
}
