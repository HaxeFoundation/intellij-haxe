package intellij.hxcpp.debug.stubs;

#if !cpp

/**
	Non-cpp stand-in for `cpp.vm.Debugger.StackFrame`, selected by the
	conditional typedef in DebuggerApi (the cpp package is rejected on other
	targets). Keep the API surface identical to the std class; the cpp compile
	of any fixture cross-checks shared code against the real type.
**/
class StackFrame {
	public var fileName(default, null):String;
	public var lineNumber(default, null):Int;
	public var className(default, null):String;
	public var functionName(default, null):String;
	public var parameters(default, null):Array<Parameter> = [];

	public function new(fileName:String, lineNumber:Int, className:String, functionName:String) {
		this.fileName = fileName;
		this.lineNumber = lineNumber;
		this.className = className;
		this.functionName = functionName;
	}
}
#end
