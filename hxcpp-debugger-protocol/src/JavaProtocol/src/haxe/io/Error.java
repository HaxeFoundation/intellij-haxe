package haxe.io;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Error extends haxe.lang.Enum
{
	static 
	{
		haxe.io.Error.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Blocked", "Overflow", "OutsideBounds", "Custom"});
		haxe.io.Error.Blocked = new haxe.io.Error(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		haxe.io.Error.Overflow = new haxe.io.Error(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		haxe.io.Error.OutsideBounds = new haxe.io.Error(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    Error(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  haxe.io.Error Blocked;
	
	public static  haxe.io.Error Overflow;
	
	public static  haxe.io.Error OutsideBounds;
	
	public static   haxe.io.Error Custom(java.lang.Object e)
	{
		return new haxe.io.Error(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{e})) ));
	}
	
	
}


