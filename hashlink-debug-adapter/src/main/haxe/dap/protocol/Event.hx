package dap.protocol;

/**
 * An adapter-initiated event (type = "event").
 */
typedef Event = {
	> ProtocolMessage,
	var event:String;
	var ?body:Dynamic;
}
