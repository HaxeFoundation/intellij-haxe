package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class HaxeException extends java.lang.RuntimeException
{
	public    HaxeException(java.lang.Object obj, java.lang.String msg, java.lang.Throwable cause)
	{
		super(msg, cause);
		if (( obj instanceof haxe.lang.HaxeException )) 
		{
			haxe.lang.HaxeException _obj = ((haxe.lang.HaxeException) (obj) );
			obj = _obj.getObject();
		}
		
		this.obj = obj;
	}
	
	
	public static   java.lang.RuntimeException wrap(java.lang.Object obj)
	{
		if (( obj instanceof java.lang.RuntimeException )) 
		{
			return ((java.lang.RuntimeException) (obj) );
		}
		
		if (( obj instanceof java.lang.String )) 
		{
			return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(obj), ((java.lang.Throwable) (null) ));
		}
		 else 
		{
			if (( obj instanceof java.lang.Throwable )) 
			{
				return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(null), ((java.lang.Throwable) (obj) ));
			}
			
		}
		
		return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(null), ((java.lang.Throwable) (null) ));
	}
	
	
	public  java.lang.Object obj;
	
	public   java.lang.Object getObject()
	{
		return this.obj;
	}
	
	
	@Override public   java.lang.String toString()
	{
		return ( "Haxe Exception: " + haxe.root.Std.string(this.obj) );
	}
	
	
}


