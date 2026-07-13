package dap.protocol;

/**
 * A response to a request (type = "response").
 * `request_seq` echoes the `seq` of the request being answered.
 */
typedef Response = {
	> ProtocolMessage,
	var request_seq:Int;
	var success:Bool;
	var command:String;
	var ?message:String;
	var ?body:Dynamic;
}
