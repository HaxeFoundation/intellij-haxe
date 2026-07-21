package dap.protocol.responses;

/**
	A structured error message (DAP "Message" type), used in error response bodies.
**/
typedef Message = {
	var id:Int;
	var format:String;
	var ?showUser:Bool;
	// DAP `Message.variables`: machine-readable details keyed by name (also the
	// substitution values for `format`). A plain JSON object on the wire.
	var ?variables:haxe.DynamicAccess<String>;
}
