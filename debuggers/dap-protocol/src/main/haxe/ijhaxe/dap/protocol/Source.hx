package ijhaxe.dap.protocol;

/**
	A source file descriptor used in breakpoint requests and responses.
**/
typedef Source = {
	var ?name:String;
	var ?path:String;
	var ?sourceReference:Int;
}
