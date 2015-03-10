package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StringList extends haxe.lang.Enum
{
	static 
	{
		debugger.StringList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Element"});
		debugger.StringList.Terminator = new debugger.StringList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    StringList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.StringList Terminator;
	
	public static   debugger.StringList Element(java.lang.String string, debugger.StringList next)
	{
		return new debugger.StringList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{string, next})) ));
	}
	
	
}


