package dap.protocol.requests;

/**
	Arguments of the "evaluate" request. This adapter evaluates VARIABLE PATHS
	(identifier + .field / [index] accessors), not arbitrary expressions.
**/
typedef EvaluateArguments = {
	var expression:String;
	var ?frameId:Null<Int>;
	var ?context:String; // "watch" | "hover" | "repl" — all treated alike
}
