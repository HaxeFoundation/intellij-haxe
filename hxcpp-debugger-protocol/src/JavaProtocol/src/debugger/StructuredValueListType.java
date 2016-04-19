// Generated by Haxe
package debugger;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class StructuredValueListType extends haxe.lang.ParamEnum
{
	public StructuredValueListType(int index, java.lang.Object[] params)
	{
		//line 100 "/usr/local/lib/haxe/std/java/internal/HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"Anonymous", "Instance", "_Array", "Class"};
	
	public static final debugger.StructuredValueListType Anonymous = new debugger.StructuredValueListType(0, null);
	
	public static debugger.StructuredValueListType Instance(java.lang.String className)
	{
		//line 303 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return new debugger.StructuredValueListType(1, new java.lang.Object[]{className});
	}
	
	
	public static final debugger.StructuredValueListType _Array = new debugger.StructuredValueListType(2, null);
	
	public static final debugger.StructuredValueListType Class = new debugger.StructuredValueListType(3, null);
	
	@Override public java.lang.String getTag()
	{
		//line 300 "/home/mike/haxe/hxcpp-debugger/debugger/IController.hx"
		return debugger.StructuredValueListType.__hx_constructs[this.index];
	}
	
	
}


