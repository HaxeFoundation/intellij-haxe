package dap.protocol;

/**
 * Arguments for the "initialize" request.
 */
typedef InitializeRequestArguments = {
	var adapterID:String;
	@:optional var clientID:String;
	@:optional var clientName:String;
	@:optional var locale:String;
	@:optional var linesStartAt1:Bool;
	@:optional var columnsStartAt1:Bool;
	@:optional var pathFormat:String;
	@:optional var supportsVariableType:Bool;
	@:optional var supportsRunInTerminalRequest:Bool;
}
