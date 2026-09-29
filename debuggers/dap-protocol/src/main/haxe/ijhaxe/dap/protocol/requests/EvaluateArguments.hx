package ijhaxe.dap.protocol.requests;

/**
	Arguments of the "evaluate" request.
**/
typedef EvaluateArguments = {
	var expression:String;
	var ?frameId:Null<Int>;
	var ?context:String; // "watch", "hover" or "repl"
}
