package dap.protocol.requests;
import dap.protocol.Source;
import dap.protocol.SourceBreakpoint;

/**
 * Arguments for the "setBreakpoints" request.
 */
typedef SetBreakpointsArguments = {
	var source:Source;
	@:optional var breakpoints:Array<SourceBreakpoint>;
	@:optional var lines:Array<Int>;
	@:optional var sourceModified:Bool;
}
