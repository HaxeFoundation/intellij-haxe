// Generated by Haxe 4.3.0
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class StructuredValueTypeList extends haxe.lang.ParamEnum
{
	public StructuredValueTypeList(int index, java.lang.Object[] params)
	{
		//line 240 "C:\\HaxeToolkit\\haxe\\std\\java\\internal\\HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Terminator", "_Type"};
	
	public static final debugger.StructuredValueTypeList Terminator = new debugger.StructuredValueTypeList(0, null);
	
	public static debugger.StructuredValueTypeList _Type(debugger.StructuredValueType type, debugger.StructuredValueTypeList next)
	{
		//line 290 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return new debugger.StructuredValueTypeList(1, new java.lang.Object[]{type, next});
	}
	
	
	@Override public java.lang.String getTag()
	{
		//line 287 "C:\\HaxeToolkit\\haxe\\lib\\hxcpp-debugger\\git\\debugger\\IController.hx"
		return debugger.StructuredValueTypeList.__hx_constructs[this.index];
	}
	
	
}

