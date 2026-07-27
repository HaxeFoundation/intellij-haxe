package ijhaxe.dap.protocol.requests;

/**
	Arguments of the "setVariable" request: set a named child of a
	`variablesReference` to `value`. `value` is a literal (number, true/false,
	null) or another variable path — no allocation, so new strings/objects
	cannot be created (see ValueWriter).
**/
typedef SetVariableArguments = {
	var variablesReference:Int;
	var name:String;
	var value:String;
}
