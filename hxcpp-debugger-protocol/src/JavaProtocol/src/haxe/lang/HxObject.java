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
package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class HxObject implements haxe.lang.IHxObject
{
	public    HxObject(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    HxObject()
	{
		haxe.lang.HxObject.__hx_ctor_haxe_lang_HxObject(this);
	}
	
	
	public static   void __hx_ctor_haxe_lang_HxObject(haxe.lang.HxObject __temp_me32)
	{
		{
		}
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.lang.HxObject(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.lang.HxObject();
	}
	
	
	public   boolean __hx_deleteField(java.lang.String field)
	{
		return false;
	}
	
	
	public   java.lang.Object __hx_lookupField(java.lang.String field, boolean throwErrors, boolean isCheck)
	{
		if (isCheck) 
		{
			return haxe.lang.Runtime.undefined;
		}
		 else 
		{
			if (throwErrors) 
			{
				throw haxe.lang.HaxeException.wrap("Field not found.");
			}
			 else 
			{
				return null;
			}
			
		}
		
	}
	
	
	public   double __hx_lookupField_f(java.lang.String field, boolean throwErrors)
	{
		if (throwErrors) 
		{
			throw haxe.lang.HaxeException.wrap("Field not found or incompatible field type.");
		}
		 else 
		{
			return 0.0;
		}
		
	}
	
	
	public   java.lang.Object __hx_lookupSetField(java.lang.String field, java.lang.Object value)
	{
		throw haxe.lang.HaxeException.wrap("Cannot access field for writing.");
	}
	
	
	public   double __hx_lookupSetField_f(java.lang.String field, double value)
	{
		throw haxe.lang.HaxeException.wrap("Cannot access field for writing or incompatible type.");
	}
	
	
	public   double __hx_setField_f(java.lang.String field, double value, boolean handleProperties)
	{
		{
			{
				return this.__hx_lookupSetField_f(field, value);
			}
			
		}
		
	}
	
	
	public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			{
				return this.__hx_lookupSetField(field, value);
			}
			
		}
		
	}
	
	
	public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			{
				return this.__hx_lookupField(field, throwErrors, isCheck);
			}
			
		}
		
	}
	
	
	public   double __hx_getField_f(java.lang.String field, boolean throwErrors, boolean handleProperties)
	{
		{
			{
				return this.__hx_lookupField_f(field, throwErrors);
			}
			
		}
		
	}
	
	
	public   java.lang.Object __hx_invokeField(java.lang.String field, haxe.root.Array dynargs)
	{
		{
			{
				return ((haxe.lang.Function) (this.__hx_getField(field, true, false, false)) ).__hx_invokeDynamic(dynargs);
			}
			
		}
		
	}
	
	
	public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		{
		}
		
	}
	
	
}


