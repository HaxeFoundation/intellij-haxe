package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StructuredValueTypeList extends haxe.lang.Enum
{
	static 
	{
		debugger.StructuredValueTypeList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "_Type"});
		debugger.StructuredValueTypeList.Terminator = new debugger.StructuredValueTypeList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    StructuredValueTypeList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.StructuredValueTypeList Terminator;
	
	public static   debugger.StructuredValueTypeList _Type(debugger.StructuredValueType type, debugger.StructuredValueTypeList next)
	{
		return new debugger.StructuredValueTypeList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{type, next})) ));
	}
	
	
}


