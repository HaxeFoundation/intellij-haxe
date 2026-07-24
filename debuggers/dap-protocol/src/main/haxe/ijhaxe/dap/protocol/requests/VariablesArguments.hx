package ijhaxe.dap.protocol.requests;

/**
	Arguments for the "variables" request.
**/
typedef VariablesArguments = {
	var variablesReference:Int;
	var ?filter:String; // "indexed" | "named"
	var ?start:Int;
	var ?count:Int;
}
