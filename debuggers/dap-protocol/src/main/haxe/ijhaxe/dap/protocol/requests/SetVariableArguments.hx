package ijhaxe.dap.protocol.requests;

/**
	Arguments of the "setVariable" request: sets the child `name` of
	`variablesReference` to `value`, an expression evaluated in the debuggee.
**/
typedef SetVariableArguments = {
	var variablesReference:Int;
	var name:String;
	var value:String;
}
