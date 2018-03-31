package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Command extends haxe.lang.Enum
{
	static 
	{
		debugger.Command.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Exit", "Detach", "Files", "AllClasses", "Classes", "Mem", "Compact", "Collect", "SetCurrentThread", "AddFileLineBreakpoint", "AddClassFunctionBreakpoint", "ListBreakpoints", "DescribeBreakpoint", "DisableAllBreakpoints", "DisableBreakpointRange", "EnableAllBreakpoints", "EnableBreakpointRange", "DeleteAllBreakpoints", "DeleteBreakpointRange", "DeleteFileLineBreakpoint", "BreakNow", "Continue", "Step", "Next", "Finish", "WhereCurrentThread", "WhereAllThreads", "Up", "Down", "SetFrame", "Variables", "PrintExpression", "SetExpression", "GetStructured"});
		debugger.Command.Exit = new debugger.Command(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.Detach = new debugger.Command(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.Files = new debugger.Command(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.AllClasses = new debugger.Command(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.Mem = new debugger.Command(((int) (5) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.Compact = new debugger.Command(((int) (6) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.Collect = new debugger.Command(((int) (7) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.DisableAllBreakpoints = new debugger.Command(((int) (13) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.EnableAllBreakpoints = new debugger.Command(((int) (15) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.DeleteAllBreakpoints = new debugger.Command(((int) (17) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.BreakNow = new debugger.Command(((int) (20) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Command.WhereAllThreads = new debugger.Command(((int) (26) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    Command(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.Command Exit;
	
	public static  debugger.Command Detach;
	
	public static  debugger.Command Files;
	
	public static  debugger.Command AllClasses;
	
	public static   debugger.Command Classes(java.lang.String continuation)
	{
		return new debugger.Command(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{continuation})) ));
	}
	
	
	public static  debugger.Command Mem;
	
	public static  debugger.Command Compact;
	
	public static  debugger.Command Collect;
	
	public static   debugger.Command SetCurrentThread(int number)
	{
		return new debugger.Command(((int) (8) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Command AddFileLineBreakpoint(java.lang.String fileName, int lineNumber)
	{
		return new debugger.Command(((int) (9) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{fileName, lineNumber})) ));
	}
	
	
	public static   debugger.Command AddClassFunctionBreakpoint(java.lang.String className, java.lang.String functionName)
	{
		return new debugger.Command(((int) (10) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className, functionName})) ));
	}
	
	
	public static   debugger.Command ListBreakpoints(boolean enabled, boolean disabled)
	{
		return new debugger.Command(((int) (11) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{enabled, disabled})) ));
	}
	
	
	public static   debugger.Command DescribeBreakpoint(int number)
	{
		return new debugger.Command(((int) (12) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static  debugger.Command DisableAllBreakpoints;
	
	public static   debugger.Command DisableBreakpointRange(int first, int last)
	{
		return new debugger.Command(((int) (14) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{first, last})) ));
	}
	
	
	public static  debugger.Command EnableAllBreakpoints;
	
	public static   debugger.Command EnableBreakpointRange(int first, int last)
	{
		return new debugger.Command(((int) (16) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{first, last})) ));
	}
	
	
	public static  debugger.Command DeleteAllBreakpoints;
	
	public static   debugger.Command DeleteBreakpointRange(int first, int last)
	{
		return new debugger.Command(((int) (18) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{first, last})) ));
	}
	
	
	public static   debugger.Command DeleteFileLineBreakpoint(java.lang.String fileName, int lineNumber)
	{
		return new debugger.Command(((int) (19) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{fileName, lineNumber})) ));
	}
	
	
	public static  debugger.Command BreakNow;
	
	public static   debugger.Command Continue(int count)
	{
		return new debugger.Command(((int) (21) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command Step(int count)
	{
		return new debugger.Command(((int) (22) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command Next(int count)
	{
		return new debugger.Command(((int) (23) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command Finish(int count)
	{
		return new debugger.Command(((int) (24) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command WhereCurrentThread(boolean unsafe)
	{
		return new debugger.Command(((int) (25) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{unsafe})) ));
	}
	
	
	public static  debugger.Command WhereAllThreads;
	
	public static   debugger.Command Up(int count)
	{
		return new debugger.Command(((int) (27) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command Down(int count)
	{
		return new debugger.Command(((int) (28) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Command SetFrame(int number)
	{
		return new debugger.Command(((int) (29) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Command Variables(boolean unsafe)
	{
		return new debugger.Command(((int) (30) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{unsafe})) ));
	}
	
	
	public static   debugger.Command PrintExpression(boolean unsafe, java.lang.String expression)
	{
		return new debugger.Command(((int) (31) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{unsafe, expression})) ));
	}
	
	
	public static   debugger.Command SetExpression(boolean unsafe, java.lang.String lhs, java.lang.String rhs)
	{
		return new debugger.Command(((int) (32) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{unsafe, lhs, rhs})) ));
	}
	
	
	public static   debugger.Command GetStructured(boolean unsafe, java.lang.String expression)
	{
		return new debugger.Command(((int) (33) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{unsafe, expression})) ));
	}
	
	
}


