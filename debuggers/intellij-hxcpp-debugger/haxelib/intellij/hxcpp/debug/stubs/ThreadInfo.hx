package intellij.hxcpp.debug.stubs;

#if !cpp

/**
	Non-cpp stand-in for `cpp.vm.Debugger.ThreadInfo`, selected by the
	conditional typedef in DebuggerApi (the cpp package is rejected on other
	targets). Keep the API surface identical to the std class; the cpp compile
	of any fixture cross-checks shared code against the real type.
**/
class ThreadInfo {
	public static inline var STATUS_RUNNING = 1;
	public static inline var STATUS_STOPPED_BREAK_IMMEDIATE = 2;
	public static inline var STATUS_STOPPED_BREAKPOINT = 3;
	public static inline var STATUS_STOPPED_UNCAUGHT_EXCEPTION = 4;
	public static inline var STATUS_STOPPED_CRITICAL_ERROR = 5;

	public var number(default, null):Int;
	public var status(default, null):Int;
	public var breakpoint(default, null):Int;
	public var criticalErrorDescription(default, null):String;
	public var stack(default, null):Array<StackFrame> = [];

	public function new(number:Int, status:Int, breakpoint:Int = -1, criticalErrorDescription:String = null) {
		this.number = number;
		this.status = status;
		this.breakpoint = breakpoint;
		this.criticalErrorDescription = criticalErrorDescription;
	}
}
#end
