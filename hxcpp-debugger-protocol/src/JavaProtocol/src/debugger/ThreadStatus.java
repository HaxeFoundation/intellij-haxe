// Generated by Haxe
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class ThreadStatus extends haxe.lang.ParamEnum
{
	public ThreadStatus(int index, java.lang.Object[] params)
	{
		//line 100 "/usr/local/lib/haxe/std/java/internal/HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Running", "StoppedImmediate", "StoppedBreakpoint", "StoppedUncaughtException", "StoppedCriticalError"};
	
	public static final debugger.ThreadStatus Running = new debugger.ThreadStatus(0, null);
	
	public static final debugger.ThreadStatus StoppedImmediate = new debugger.ThreadStatus(1, null);
	
	public static debugger.ThreadStatus StoppedBreakpoint(int number)
	{
		//line 239 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.ThreadStatus(2, new java.lang.Object[]{number});
	}
	
	
	public static final debugger.ThreadStatus StoppedUncaughtException = new debugger.ThreadStatus(3, null);
	
	public static debugger.ThreadStatus StoppedCriticalError(java.lang.String description)
	{
		//line 241 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.ThreadStatus(4, new java.lang.Object[]{description});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 235 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return debugger.ThreadStatus.__hx_constructs[this.index];
	}
	
	
}


