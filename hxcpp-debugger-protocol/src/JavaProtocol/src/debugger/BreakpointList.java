package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class BreakpointList extends haxe.lang.Enum
{
	static 
	{
		debugger.BreakpointList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Breakpoint"});
		debugger.BreakpointList.Terminator = new debugger.BreakpointList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    BreakpointList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.BreakpointList Terminator;
	
	public static   debugger.BreakpointList Breakpoint(int number, java.lang.String description, boolean enabled, boolean multi, debugger.BreakpointList next)
	{
		return new debugger.BreakpointList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, description, enabled, multi, next})) ));
	}
	
	
}


