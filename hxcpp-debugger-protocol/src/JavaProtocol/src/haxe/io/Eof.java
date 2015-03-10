package haxe.io;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Eof extends haxe.lang.HxObject
{
	public    Eof(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Eof()
	{
		haxe.io.Eof.__hx_ctor_haxe_io_Eof(this);
	}
	
	
	public static   void __hx_ctor_haxe_io_Eof(haxe.io.Eof __temp_me30)
	{
		{
		}
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.io.Eof(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.io.Eof();
	}
	
	
	@Override public   java.lang.String toString()
	{
		return "Eof";
	}
	
	
	@Override public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			boolean __temp_executeDef190 = true;
			switch (field.hashCode())
			{
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef190 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("toString"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef190) 
			{
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   java.lang.Object __hx_invokeField(java.lang.String field, haxe.root.Array dynargs)
	{
		{
			boolean __temp_executeDef191 = true;
			switch (field.hashCode())
			{
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef191 = false;
						return this.toString();
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef191) 
			{
				return super.__hx_invokeField(field, dynargs);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
}


