/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package haxe.root;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class List<T> extends haxe.lang.HxObject
{
	public    List(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    List()
	{
		haxe.root.List.__hx_ctor__List(this);
	}
	
	
	public static  <T_c> void __hx_ctor__List(haxe.root.List<T_c> __temp_me17)
	{
		__temp_me17.length = 0;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.List<java.lang.Object>(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.List<java.lang.Object>();
	}
	
	
	public  haxe.root.Array h;
	
	public  haxe.root.Array q;
	
	public  int length;
	
	public   void add(T item)
	{
		haxe.root.Array x = new haxe.root.Array(new java.lang.Object[]{item});
		if (( this.h == null )) 
		{
			this.h = x;
		}
		 else 
		{
			this.q.__set(1, x);
		}
		
		this.q = x;
		this.length++;
	}
	
	
	public   java.lang.Object iterator()
	{
		haxe.root.Array<haxe.root.Array> h = new haxe.root.Array<haxe.root.Array>(new haxe.root.Array[]{this.h});
		java.lang.Object __temp_stmt71 = null;
		{
			haxe.lang.Function __temp_odecl69 = new haxe.root.List_iterator_165__Fun(((haxe.root.Array<haxe.root.Array>) (h) ));
			haxe.lang.Function __temp_odecl70 = new haxe.root.List_iterator_168__Fun(((haxe.root.Array<haxe.root.Array>) (h) ));
			__temp_stmt71 = new haxe.lang.DynamicObject(new haxe.root.Array<java.lang.String>(new java.lang.String[]{"hasNext", "next"}), new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{__temp_odecl69, __temp_odecl70}), new haxe.root.Array<java.lang.String>(new java.lang.String[]{}), new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{}));
		}
		
		return ((java.lang.Object) (__temp_stmt71) );
	}
	
	
	@Override public   double __hx_setField_f(java.lang.String field, double value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef72 = true;
			switch (field.hashCode())
			{
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef72 = false;
						this.length = ((int) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef72) 
			{
				return super.__hx_setField_f(field, value, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef73 = true;
			switch (field.hashCode())
			{
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef73 = false;
						this.length = ((int) (haxe.lang.Runtime.toInt(value)) );
						return value;
					}
					
					break;
				}
				
				
				case 104:
				{
					if (field.equals("h")) 
					{
						__temp_executeDef73 = false;
						this.h = ((haxe.root.Array) (value) );
						return value;
					}
					
					break;
				}
				
				
				case 113:
				{
					if (field.equals("q")) 
					{
						__temp_executeDef73 = false;
						this.q = ((haxe.root.Array) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef73) 
			{
				return super.__hx_setField(field, value, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			boolean __temp_executeDef74 = true;
			switch (field.hashCode())
			{
				case 1182533742:
				{
					if (field.equals("iterator")) 
					{
						__temp_executeDef74 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("iterator"))) );
					}
					
					break;
				}
				
				
				case 104:
				{
					if (field.equals("h")) 
					{
						__temp_executeDef74 = false;
						return this.h;
					}
					
					break;
				}
				
				
				case 96417:
				{
					if (field.equals("add")) 
					{
						__temp_executeDef74 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("add"))) );
					}
					
					break;
				}
				
				
				case 113:
				{
					if (field.equals("q")) 
					{
						__temp_executeDef74 = false;
						return this.q;
					}
					
					break;
				}
				
				
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef74 = false;
						return this.length;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef74) 
			{
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   double __hx_getField_f(java.lang.String field, boolean throwErrors, boolean handleProperties)
	{
		{
			boolean __temp_executeDef75 = true;
			switch (field.hashCode())
			{
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef75 = false;
						return ((double) (this.length) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef75) 
			{
				return super.__hx_getField_f(field, throwErrors, handleProperties);
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
			boolean __temp_executeDef76 = true;
			switch (field.hashCode())
			{
				case 1182533742:
				{
					if (field.equals("iterator")) 
					{
						__temp_executeDef76 = false;
						return this.iterator();
					}
					
					break;
				}
				
				
				case 96417:
				{
					if (field.equals("add")) 
					{
						__temp_executeDef76 = false;
						this.add(((T) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef76) 
			{
				return super.__hx_invokeField(field, dynargs);
			}
			
		}
		
		return null;
	}
	
	
	@Override public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		baseArr.push("length");
		baseArr.push("q");
		baseArr.push("h");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


