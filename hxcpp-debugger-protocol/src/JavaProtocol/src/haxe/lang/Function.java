package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Function
{
	public    Function(int arity, int type)
	{
		this.__fn_type = type;
		this.__hx_arity = arity;
	}
	
	
	public   java.lang.Object __hx_invokeDynamic(haxe.root.Array __fn_dynargs)
	{
		switch (( (( __fn_dynargs == null )) ? (0) : (__fn_dynargs.length) ))
		{
			case 0:
			{
				return this.__hx_invoke0_o();
			}
			
			
			case 1:
			{
				return this.__hx_invoke1_o(0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ));
			}
			
			
			case 2:
			{
				return this.__hx_invoke2_o(0.0, 0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ), ((java.lang.Object) (__fn_dynargs.__get(1)) ));
			}
			
			
			case 3:
			{
				return this.__hx_invoke3_o(0.0, 0.0, 0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ), ((java.lang.Object) (__fn_dynargs.__get(1)) ), ((java.lang.Object) (__fn_dynargs.__get(2)) ));
			}
			
			
			case 4:
			{
				return this.__hx_invoke4_o(0.0, 0.0, 0.0, 0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ), ((java.lang.Object) (__fn_dynargs.__get(1)) ), ((java.lang.Object) (__fn_dynargs.__get(2)) ), ((java.lang.Object) (__fn_dynargs.__get(3)) ));
			}
			
			
			case 5:
			{
				return this.__hx_invoke5_o(0.0, 0.0, 0.0, 0.0, 0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ), ((java.lang.Object) (__fn_dynargs.__get(1)) ), ((java.lang.Object) (__fn_dynargs.__get(2)) ), ((java.lang.Object) (__fn_dynargs.__get(3)) ), ((java.lang.Object) (__fn_dynargs.__get(4)) ));
			}
			
			
			case 6:
			{
				return this.__hx_invoke6_o(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, ((java.lang.Object) (__fn_dynargs.__get(0)) ), ((java.lang.Object) (__fn_dynargs.__get(1)) ), ((java.lang.Object) (__fn_dynargs.__get(2)) ), ((java.lang.Object) (__fn_dynargs.__get(3)) ), ((java.lang.Object) (__fn_dynargs.__get(4)) ), ((java.lang.Object) (__fn_dynargs.__get(5)) ));
			}
			
			
			default:
			{
				throw haxe.lang.HaxeException.wrap("Too many arguments");
			}
			
		}
		
	}
	
	
	public  int __fn_type;
	
	public  int __hx_arity;
	
	public   java.lang.Object __hx_invoke6_o(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, double __fn_float5, double __fn_float6, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4, java.lang.Object __fn_dyn5, java.lang.Object __fn_dyn6)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke6_f(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_float5, __fn_float6, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4, __fn_dyn5, __fn_dyn6);
		}
		
	}
	
	
	public   double __hx_invoke6_f(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, double __fn_float5, double __fn_float6, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4, java.lang.Object __fn_dyn5, java.lang.Object __fn_dyn6)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke6_o(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_float5, __fn_float6, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4, __fn_dyn5, __fn_dyn6))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke5_o(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, double __fn_float5, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4, java.lang.Object __fn_dyn5)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke5_f(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_float5, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4, __fn_dyn5);
		}
		
	}
	
	
	public   double __hx_invoke5_f(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, double __fn_float5, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4, java.lang.Object __fn_dyn5)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke5_o(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_float5, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4, __fn_dyn5))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke4_o(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke4_f(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4);
		}
		
	}
	
	
	public   double __hx_invoke4_f(double __fn_float1, double __fn_float2, double __fn_float3, double __fn_float4, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3, java.lang.Object __fn_dyn4)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke4_o(__fn_float1, __fn_float2, __fn_float3, __fn_float4, __fn_dyn1, __fn_dyn2, __fn_dyn3, __fn_dyn4))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke3_o(double __fn_float1, double __fn_float2, double __fn_float3, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke3_f(__fn_float1, __fn_float2, __fn_float3, __fn_dyn1, __fn_dyn2, __fn_dyn3);
		}
		
	}
	
	
	public   double __hx_invoke3_f(double __fn_float1, double __fn_float2, double __fn_float3, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2, java.lang.Object __fn_dyn3)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke3_o(__fn_float1, __fn_float2, __fn_float3, __fn_dyn1, __fn_dyn2, __fn_dyn3))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke2_o(double __fn_float1, double __fn_float2, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke2_f(__fn_float1, __fn_float2, __fn_dyn1, __fn_dyn2);
		}
		
	}
	
	
	public   double __hx_invoke2_f(double __fn_float1, double __fn_float2, java.lang.Object __fn_dyn1, java.lang.Object __fn_dyn2)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke2_o(__fn_float1, __fn_float2, __fn_dyn1, __fn_dyn2))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke1_o(double __fn_float1, java.lang.Object __fn_dyn1)
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke1_f(__fn_float1, __fn_dyn1);
		}
		
	}
	
	
	public   double __hx_invoke1_f(double __fn_float1, java.lang.Object __fn_dyn1)
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke1_o(__fn_float1, __fn_dyn1))) );
		}
		
	}
	
	
	public   java.lang.Object __hx_invoke0_o()
	{
		if (( this.__fn_type != 1 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return this.__hx_invoke0_f();
		}
		
	}
	
	
	public   double __hx_invoke0_f()
	{
		if (( this.__fn_type != 0 )) 
		{
			throw haxe.lang.HaxeException.wrap("Wrong number of arguments");
		}
		 else 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_invoke0_o())) );
		}
		
	}
	
	
}


