package ijhaxe.dap.protocol.requests;
import ijhaxe.dap.protocol.Source;
import ijhaxe.dap.protocol.SourceBreakpoint;

/**
	Arguments for the "setBreakpoints" request.
**/
typedef SetBreakpointsArguments = {
	var source:Source;
	var ?breakpoints:Array<SourceBreakpoint>;
	var ?lines:Array<Int>;
	var ?sourceModified:Bool;
}
