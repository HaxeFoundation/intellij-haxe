package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class BreakpointLocationList extends haxe.lang.Enum
{
	static 
	{
		debugger.BreakpointLocationList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "FileLine", "ClassFunction"});
		debugger.BreakpointLocationList.Terminator = new debugger.BreakpointLocationList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    BreakpointLocationList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.BreakpointLocationList Terminator;
	
	public static   debugger.BreakpointLocationList FileLine(java.lang.String fileName, int lineNumber, debugger.BreakpointLocationList next)
	{
		return new debugger.BreakpointLocationList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{fileName, lineNumber, next})) ));
	}
	
	
	public static   debugger.BreakpointLocationList ClassFunction(java.lang.String className, java.lang.String functionName, debugger.BreakpointLocationList next)
	{
		return new debugger.BreakpointLocationList(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className, functionName, next})) ));
	}
	
	
}


