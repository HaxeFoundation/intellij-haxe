// Generated by Haxe 4.3.0
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class Message extends haxe.lang.ParamEnum
{
	public Message(int index, java.lang.Object[] params)
	{
		//line 240 "C:\\HaxeToolkit\\haxe\\std\\java\\internal\\HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"ErrorInternal", "ErrorNoSuchThread", "ErrorNoSuchFile", "ErrorNoSuchBreakpoint", "ErrorBadClassNameRegex", "ErrorBadFunctionNameRegex", "ErrorNoMatchingFunctions", "ErrorBadCount", "ErrorCurrentThreadNotStopped", "ErrorEvaluatingExpression", "OK", "Exited", "Detached", "Files", "AllClasses", "Classes", "MemBytes", "Compacted", "Collected", "ThreadLocation", "FileLineBreakpointNumber", "ClassFunctionBreakpointNumber", "Breakpoints", "BreakpointDescription", "BreakpointStatuses", "ThreadsWhere", "Variables", "Value", "Structured", "ThreadCreated", "ThreadTerminated", "ThreadStarted", "ThreadStopped"};
	
	public static debugger.Message ErrorInternal(java.lang.String details)
	{
		//line 340 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(0, new java.lang.Object[]{details});
	}
	
	
	public static debugger.Message ErrorNoSuchThread(int number)
	{
		//line 341 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(1, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ErrorNoSuchFile(java.lang.String fileName)
	{
		//line 342 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(2, new java.lang.Object[]{fileName});
	}
	
	
	public static debugger.Message ErrorNoSuchBreakpoint(int number)
	{
		//line 343 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(3, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ErrorBadClassNameRegex(java.lang.String details)
	{
		//line 344 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(4, new java.lang.Object[]{details});
	}
	
	
	public static debugger.Message ErrorBadFunctionNameRegex(java.lang.String details)
	{
		//line 345 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(5, new java.lang.Object[]{details});
	}
	
	
	public static debugger.Message ErrorNoMatchingFunctions(java.lang.String className, java.lang.String functionName, debugger.StringList unresolvableClasses)
	{
		//line 346 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(6, new java.lang.Object[]{className, functionName, unresolvableClasses});
	}
	
	
	public static debugger.Message ErrorBadCount(int count)
	{
		//line 348 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(7, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Message ErrorCurrentThreadNotStopped(int threadNumber)
	{
		//line 349 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(8, new java.lang.Object[]{threadNumber});
	}
	
	
	public static debugger.Message ErrorEvaluatingExpression(java.lang.String details)
	{
		//line 350 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(9, new java.lang.Object[]{details});
	}
	
	
	public static final debugger.Message OK = new debugger.Message(10, null);
	
	public static final debugger.Message Exited = new debugger.Message(11, null);
	
	public static final debugger.Message Detached = new debugger.Message(12, null);
	
	public static debugger.Message Files(debugger.StringList list)
	{
		//line 356 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(13, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message AllClasses(debugger.StringList list)
	{
		//line 357 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(14, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message Classes(debugger.ClassList list)
	{
		//line 358 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(15, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message MemBytes(int bytes)
	{
		//line 359 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(16, new java.lang.Object[]{bytes});
	}
	
	
	public static debugger.Message Compacted(int bytesBefore, int bytesAfter)
	{
		//line 360 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(17, new java.lang.Object[]{bytesBefore, bytesAfter});
	}
	
	
	public static debugger.Message Collected(int bytesBefore, int bytesAfter)
	{
		//line 361 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(18, new java.lang.Object[]{bytesBefore, bytesAfter});
	}
	
	
	public static debugger.Message ThreadLocation(int number, int stackFrame, java.lang.String className, java.lang.String functionName, java.lang.String fileName, int lineNumber)
	{
		//line 362 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(19, new java.lang.Object[]{number, stackFrame, className, functionName, fileName, lineNumber});
	}
	
	
	public static debugger.Message FileLineBreakpointNumber(int number)
	{
		//line 364 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(20, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ClassFunctionBreakpointNumber(int number, debugger.StringList unresolvableClasses)
	{
		//line 365 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(21, new java.lang.Object[]{number, unresolvableClasses});
	}
	
	
	public static debugger.Message Breakpoints(debugger.BreakpointList list)
	{
		//line 367 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(22, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message BreakpointDescription(int number, debugger.BreakpointLocationList list)
	{
		//line 368 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(23, new java.lang.Object[]{number, list});
	}
	
	
	public static debugger.Message BreakpointStatuses(debugger.BreakpointStatusList list)
	{
		//line 369 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(24, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message ThreadsWhere(debugger.ThreadWhereList list)
	{
		//line 370 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(25, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message Variables(debugger.StringList list)
	{
		//line 371 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(26, new java.lang.Object[]{list});
	}
	
	
	public static debugger.Message Value(java.lang.String expression, java.lang.String type, java.lang.String value)
	{
		//line 372 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(27, new java.lang.Object[]{expression, type, value});
	}
	
	
	public static debugger.Message Structured(debugger.StructuredValue structuredValue)
	{
		//line 373 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(28, new java.lang.Object[]{structuredValue});
	}
	
	
	public static debugger.Message ThreadCreated(int number)
	{
		//line 376 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(29, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ThreadTerminated(int number)
	{
		//line 377 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(30, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ThreadStarted(int number)
	{
		//line 378 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(31, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Message ThreadStopped(int number, int stackFrame, java.lang.String className, java.lang.String functionName, java.lang.String fileName, int lineNumber)
	{
		//line 379 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.Message(32, new java.lang.Object[]{number, stackFrame, className, functionName, fileName, lineNumber});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 337 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return debugger.Message.__hx_constructs[this.index];
	}
	
	
}


