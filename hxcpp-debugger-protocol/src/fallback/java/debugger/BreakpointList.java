// Generated by Haxe 4.3.0
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class BreakpointList extends haxe.lang.ParamEnum
{
	public BreakpointList(int index, java.lang.Object[] params)
	{
		//line 240 "C:\\HaxeToolkit\\haxe\\std\\java\\internal\\HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Terminator", "Breakpoint"};
	
	public static final debugger.BreakpointList Terminator = new debugger.BreakpointList(0, null);
	
	public static debugger.BreakpointList Breakpoint(int number, java.lang.String description, boolean enabled, boolean multi, debugger.BreakpointList next)
	{
		//line 195 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.BreakpointList(1, new java.lang.Object[]{number, description, enabled, multi, next});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 192 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return debugger.BreakpointList.__hx_constructs[this.index];
	}
	
	
}


