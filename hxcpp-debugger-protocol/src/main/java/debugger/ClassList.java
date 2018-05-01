package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class ClassList extends haxe.lang.Enum
{
	static 
	{
		debugger.ClassList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Continued", "Element"});
		debugger.ClassList.Terminator = new debugger.ClassList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    ClassList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.ClassList Terminator;
	
	public static   debugger.ClassList Continued(java.lang.String continuation)
	{
		return new debugger.ClassList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{continuation})) ));
	}
	
	
	public static   debugger.ClassList Element(java.lang.String className, boolean hasStatics, debugger.ClassList next)
	{
		return new debugger.ClassList(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className, hasStatics, next})) ));
	}
	
	
}


