package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StructuredValueListType extends haxe.lang.Enum
{
	static 
	{
		debugger.StructuredValueListType.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Anonymous", "Instance", "_Array", "Class"});
		debugger.StructuredValueListType.Anonymous = new debugger.StructuredValueListType(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueListType._Array = new debugger.StructuredValueListType(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueListType.Class = new debugger.StructuredValueListType(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    StructuredValueListType(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.StructuredValueListType Anonymous;
	
	public static   debugger.StructuredValueListType Instance(java.lang.String className)
	{
		return new debugger.StructuredValueListType(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className})) ));
	}
	
	
	public static  debugger.StructuredValueListType _Array;
	
	public static  debugger.StructuredValueListType Class;
	
}


