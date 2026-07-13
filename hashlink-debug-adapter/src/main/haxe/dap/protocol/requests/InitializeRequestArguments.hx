package dap.protocol.requests;

/**
 * Arguments for the "initialize" request.
 */
typedef InitializeRequestArguments = {
	var adapterID:String;
	var ?clientID:String;
	var ?clientName:String;
	var ?locale:String;
	var ?linesStartAt1:Bool;
	var ?columnsStartAt1:Bool;
	var ?pathFormat:String;
	var ?supportsVariableType:Bool;
	var ?supportsRunInTerminalRequest:Bool;
}
