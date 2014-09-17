package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StructuredValue extends haxe.lang.Enum
{
	static 
	{
		debugger.StructuredValue.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Elided", "Single", "List"});
	}
	public    StructuredValue(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static   debugger.StructuredValue Elided(debugger.StructuredValueType type, java.lang.String getExpression)
	{
		return new debugger.StructuredValue(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{type, getExpression})) ));
	}
	
	
	public static   debugger.StructuredValue Single(debugger.StructuredValueType type, java.lang.String value)
	{
		return new debugger.StructuredValue(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{type, value})) ));
	}
	
	
	public static   debugger.StructuredValue List(debugger.StructuredValueListType type, debugger.StructuredValueList list)
	{
		return new debugger.StructuredValue(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{type, list})) ));
	}
	
	
}


