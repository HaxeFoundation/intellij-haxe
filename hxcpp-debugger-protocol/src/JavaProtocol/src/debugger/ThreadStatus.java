package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class ThreadStatus extends haxe.lang.Enum
{
	static 
	{
		debugger.ThreadStatus.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Running", "StoppedImmediate", "StoppedBreakpoint", "StoppedUncaughtException", "StoppedCriticalError"});
		debugger.ThreadStatus.Running = new debugger.ThreadStatus(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.ThreadStatus.StoppedImmediate = new debugger.ThreadStatus(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.ThreadStatus.StoppedUncaughtException = new debugger.ThreadStatus(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    ThreadStatus(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.ThreadStatus Running;
	
	public static  debugger.ThreadStatus StoppedImmediate;
	
	public static   debugger.ThreadStatus StoppedBreakpoint(int number)
	{
		return new debugger.ThreadStatus(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static  debugger.ThreadStatus StoppedUncaughtException;
	
	public static   debugger.ThreadStatus StoppedCriticalError(java.lang.String description)
	{
		return new debugger.ThreadStatus(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{description})) ));
	}
	
	
}


