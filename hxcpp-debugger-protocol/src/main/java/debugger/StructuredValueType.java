package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StructuredValueType extends haxe.lang.Enum
{
	static 
	{
		debugger.StructuredValueType.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"TypeNull", "TypeBool", "TypeInt", "TypeFloat", "TypeString", "TypeInstance", "TypeEnum", "TypeAnonymous", "TypeClass", "TypeFunction", "TypeArray"});
		debugger.StructuredValueType.TypeNull = new debugger.StructuredValueType(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeBool = new debugger.StructuredValueType(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeInt = new debugger.StructuredValueType(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeFloat = new debugger.StructuredValueType(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeString = new debugger.StructuredValueType(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeFunction = new debugger.StructuredValueType(((int) (9) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.StructuredValueType.TypeArray = new debugger.StructuredValueType(((int) (10) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    StructuredValueType(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.StructuredValueType TypeNull;
	
	public static  debugger.StructuredValueType TypeBool;
	
	public static  debugger.StructuredValueType TypeInt;
	
	public static  debugger.StructuredValueType TypeFloat;
	
	public static  debugger.StructuredValueType TypeString;
	
	public static   debugger.StructuredValueType TypeInstance(java.lang.String className)
	{
		return new debugger.StructuredValueType(((int) (5) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className})) ));
	}
	
	
	public static   debugger.StructuredValueType TypeEnum(java.lang.String enumName)
	{
		return new debugger.StructuredValueType(((int) (6) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{enumName})) ));
	}
	
	
	public static   debugger.StructuredValueType TypeAnonymous(debugger.StructuredValueTypeList elements)
	{
		return new debugger.StructuredValueType(((int) (7) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{elements})) ));
	}
	
	
	public static   debugger.StructuredValueType TypeClass(java.lang.String className)
	{
		return new debugger.StructuredValueType(((int) (8) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className})) ));
	}
	
	
	public static  debugger.StructuredValueType TypeFunction;
	
	public static  debugger.StructuredValueType TypeArray;
	
}


