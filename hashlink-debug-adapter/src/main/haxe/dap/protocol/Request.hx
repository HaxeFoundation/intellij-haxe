package dap.protocol;

/**
 * A client-initiated request (type = "request").
 */
typedef Request = {
	> ProtocolMessage,
	var command:String;
	@:optional var arguments:Dynamic;
}
