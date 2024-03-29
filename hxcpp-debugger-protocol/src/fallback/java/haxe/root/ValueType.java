// Generated by Haxe 4.3.0
package haxe.root;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class ValueType extends haxe.lang.ParamEnum
{
	public ValueType(int index, java.lang.Object[] params)
	{
		//line 240 "C:\\HaxeToolkit\\haxe\\std\\java\\internal\\HxObject.hx"
		super(index, params);
	}
	
	
	public static final java.lang.String[] __hx_constructs = new java.lang.String[]{"TNull", "TInt", "TFloat", "TBool", "TObject", "TFunction", "TClass", "TEnum", "TUnknown"};
	
	public static final haxe.root.ValueType TNull = new haxe.root.ValueType(0, null);
	
	public static final haxe.root.ValueType TInt = new haxe.root.ValueType(1, null);
	
	public static final haxe.root.ValueType TFloat = new haxe.root.ValueType(2, null);
	
	public static final haxe.root.ValueType TBool = new haxe.root.ValueType(3, null);
	
	public static final haxe.root.ValueType TObject = new haxe.root.ValueType(4, null);
	
	public static final haxe.root.ValueType TFunction = new haxe.root.ValueType(5, null);
	
	public static haxe.root.ValueType TClass(java.lang.Class c)
	{
		//line 34 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\Type.hx"
		return new haxe.root.ValueType(6, new java.lang.Object[]{c});
	}
	
	
	public static haxe.root.ValueType TEnum(java.lang.Class e)
	{
		//line 35 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\Type.hx"
		return new haxe.root.ValueType(7, new java.lang.Object[]{e});
	}
	
	
	public static final haxe.root.ValueType TUnknown = new haxe.root.ValueType(8, null);
	
	@Override public java.lang.String getTag()
	{
		//line 27 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\Type.hx"
		return haxe.root.ValueType.__hx_constructs[this.index];
	}
	
	
}


