// Generated by Haxe 4.3.0
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class StructuredValueList extends haxe.lang.ParamEnum
{
	public StructuredValueList(int index, java.lang.Object[] params)
	{
		//line 240 "C:\\HaxeToolkit\\haxe\\std\\java\\internal\\HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Terminator", "Element"};
	
	public static final debugger.StructuredValueList Terminator = new debugger.StructuredValueList(0, null);
	
	public static debugger.StructuredValueList Element(java.lang.String name, debugger.StructuredValue value, debugger.StructuredValueList next)
	{
		//line 312 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.StructuredValueList(1, new java.lang.Object[]{name, value, next});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 309 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return debugger.StructuredValueList.__hx_constructs[this.index];
	}
	
	
}


