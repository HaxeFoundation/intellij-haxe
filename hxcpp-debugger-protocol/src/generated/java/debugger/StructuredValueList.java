package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StructuredValueList extends haxe.lang.Enum
{
	static 
	{
		debugger.StructuredValueList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Element"});
		debugger.StructuredValueList.Terminator = new debugger.StructuredValueList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    StructuredValueList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.StructuredValueList Terminator;
	
	public static   debugger.StructuredValueList Element(java.lang.String name, debugger.StructuredValue value, debugger.StructuredValueList next)
	{
		return new debugger.StructuredValueList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{name, value, next})) ));
	}
	
	
}


