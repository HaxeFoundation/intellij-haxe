package dap.protocol;

/**
 * A structured error message (DAP "Message" type), used in error response bodies.
 */
typedef Message = {
	var id:Int;
	var format:String;
	@:optional var showUser:Bool;
}
