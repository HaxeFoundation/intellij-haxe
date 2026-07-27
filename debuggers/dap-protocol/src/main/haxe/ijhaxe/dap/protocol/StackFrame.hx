package ijhaxe.dap.protocol;

/**
	One frame in a "stackTrace" response.
**/
typedef StackFrame = {
	var id:Int;
	var name:String;
	var line:Int;
	var column:Int;
	var ?source:Source;
}
