// Generated by Haxe
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class Command extends haxe.lang.ParamEnum
{
	public Command(int index, java.lang.Object[] params)
	{
		//line 100 "/usr/local/lib/haxe/std/java/internal/HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Exit", "Detach", "Files", "FilesFullPath", "AllClasses", "Classes", "Mem", "Compact", "Collect", "SetCurrentThread", "AddFileLineBreakpoint", "AddClassFunctionBreakpoint", "ListBreakpoints", "DescribeBreakpoint", "DisableAllBreakpoints", "DisableBreakpointRange", "EnableAllBreakpoints", "EnableBreakpointRange", "DeleteAllBreakpoints", "DeleteBreakpointRange", "DeleteFileLineBreakpoint", "BreakNow", "Continue", "Step", "Next", "Finish", "WhereCurrentThread", "WhereAllThreads", "Up", "Down", "SetFrame", "Variables", "PrintExpression", "SetExpression", "GetStructured", "NextLine"};
	
	public static final debugger.Command Exit = new debugger.Command(0, null);
	
	public static final debugger.Command Detach = new debugger.Command(1, null);
	
	public static final debugger.Command Files = new debugger.Command(2, null);
	
	public static final debugger.Command FilesFullPath = new debugger.Command(3, null);
	
	public static final debugger.Command AllClasses = new debugger.Command(4, null);
	
	public static debugger.Command Classes(java.lang.String continuation)
	{
		//line 73 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(5, new java.lang.Object[]{continuation});
	}
	
	
	public static final debugger.Command Mem = new debugger.Command(6, null);
	
	public static final debugger.Command Compact = new debugger.Command(7, null);
	
	public static final debugger.Command Collect = new debugger.Command(8, null);
	
	public static debugger.Command SetCurrentThread(int number)
	{
		//line 85 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(9, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Command AddFileLineBreakpoint(java.lang.String fileName, int lineNumber, int columnNumber)
	{
		//line 88 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(10, new java.lang.Object[]{fileName, lineNumber, columnNumber});
	}
	
	
	public static debugger.Command AddClassFunctionBreakpoint(java.lang.String className, java.lang.String functionName)
	{
		//line 91 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(11, new java.lang.Object[]{className, functionName});
	}
	
	
	public static debugger.Command ListBreakpoints(boolean enabled, boolean disabled)
	{
		//line 95 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(12, new java.lang.Object[]{enabled, disabled});
	}
	
	
	public static debugger.Command DescribeBreakpoint(int number)
	{
		//line 98 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(13, new java.lang.Object[]{number});
	}
	
	
	public static final debugger.Command DisableAllBreakpoints = new debugger.Command(14, null);
	
	public static debugger.Command DisableBreakpointRange(int first, int last)
	{
		//line 104 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(15, new java.lang.Object[]{first, last});
	}
	
	
	public static final debugger.Command EnableAllBreakpoints = new debugger.Command(16, null);
	
	public static debugger.Command EnableBreakpointRange(int first, int last)
	{
		//line 110 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(17, new java.lang.Object[]{first, last});
	}
	
	
	public static final debugger.Command DeleteAllBreakpoints = new debugger.Command(18, null);
	
	public static debugger.Command DeleteBreakpointRange(int first, int last)
	{
		//line 116 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(19, new java.lang.Object[]{first, last});
	}
	
	
	public static debugger.Command DeleteFileLineBreakpoint(java.lang.String fileName, int lineNumber, int columnNumber)
	{
		//line 119 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(20, new java.lang.Object[]{fileName, lineNumber, columnNumber});
	}
	
	
	public static final debugger.Command BreakNow = new debugger.Command(21, null);
	
	public static debugger.Command Continue(int count)
	{
		//line 125 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(22, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command Step(int count)
	{
		//line 128 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(23, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command Next(int count)
	{
		//line 131 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(24, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command Finish(int count)
	{
		//line 134 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(25, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command WhereCurrentThread(boolean unsafe)
	{
		//line 137 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(26, new java.lang.Object[]{unsafe});
	}
	
	
	public static final debugger.Command WhereAllThreads = new debugger.Command(27, null);
	
	public static debugger.Command Up(int count)
	{
		//line 143 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(28, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command Down(int count)
	{
		//line 146 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(29, new java.lang.Object[]{count});
	}
	
	
	public static debugger.Command SetFrame(int number)
	{
		//line 149 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(30, new java.lang.Object[]{number});
	}
	
	
	public static debugger.Command Variables(boolean unsafe)
	{
		//line 152 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(31, new java.lang.Object[]{unsafe});
	}
	
	
	public static debugger.Command PrintExpression(boolean unsafe, java.lang.String expression)
	{
		//line 155 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(32, new java.lang.Object[]{unsafe, expression});
	}
	
	
	public static debugger.Command SetExpression(boolean unsafe, java.lang.String lhs, java.lang.String rhs)
	{
		//line 158 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(33, new java.lang.Object[]{unsafe, lhs, rhs});
	}
	
	
	public static debugger.Command GetStructured(boolean unsafe, java.lang.String expression)
	{
		//line 161 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(34, new java.lang.Object[]{unsafe, expression});
	}
	
	
	public static debugger.Command NextLine(int count)
	{
		//line 164 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.Command(35, new java.lang.Object[]{count});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 56 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return debugger.Command.__hx_constructs[this.index];
	}
	
	
}


