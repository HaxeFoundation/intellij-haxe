package dap.protocol.requests;
import dap.protocol.Source;
import dap.protocol.SourceBreakpoint;

/**
	Arguments for the "setBreakpoints" request.
**/
typedef SetBreakpointsArguments = {
	var source:Source;
	var ?breakpoints:Array<SourceBreakpoint>;
	var ?lines:Array<Int>;
	var ?sourceModified:Bool;
}
