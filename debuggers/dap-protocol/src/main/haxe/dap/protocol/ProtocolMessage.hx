package dap.protocol;

/**
	Base type of all DAP messages.
	`type` is one of "request", "response" or "event".
**/
typedef ProtocolMessage = {
	var seq:Int;
	var type:String;
}
