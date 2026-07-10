package dap.protocol;

/**
 * A source file descriptor used in breakpoint requests and responses.
 */
typedef Source = {
	@:optional var name:String;
	@:optional var path:String;
	@:optional var sourceReference:Int;
}
