package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class BreakpointStatusList extends haxe.lang.Enum
{
	static 
	{
		debugger.BreakpointStatusList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Nonexistent", "Disabled", "AlreadyDisabled", "Enabled", "AlreadyEnabled", "Deleted"});
		debugger.BreakpointStatusList.Terminator = new debugger.BreakpointStatusList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    BreakpointStatusList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.BreakpointStatusList Terminator;
	
	public static   debugger.BreakpointStatusList Nonexistent(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
	public static   debugger.BreakpointStatusList Disabled(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
	public static   debugger.BreakpointStatusList AlreadyDisabled(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
	public static   debugger.BreakpointStatusList Enabled(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
	public static   debugger.BreakpointStatusList AlreadyEnabled(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (5) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
	public static   debugger.BreakpointStatusList Deleted(int number, debugger.BreakpointStatusList next)
	{
		return new debugger.BreakpointStatusList(((int) (6) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, next})) ));
	}
	
	
}


