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
package _JavaProtocol;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class InputAdapter extends haxe.io.Input
{
	public    InputAdapter(haxe.lang.EmptyObject empty)
	{
		super(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public    InputAdapter(java.io.InputStream is)
	{
		_JavaProtocol.InputAdapter.__hx_ctor__JavaProtocol_InputAdapter(this, is);
	}
	
	
	public static   void __hx_ctor__JavaProtocol_InputAdapter(_JavaProtocol.InputAdapter __temp_me16, java.io.InputStream is)
	{
		__temp_me16.mIs = is;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new _JavaProtocol.InputAdapter(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new _JavaProtocol.InputAdapter(((java.io.InputStream) (arr.__get(0)) ));
	}
	
	
	@Override public   int readBytes(haxe.io.Bytes bytes, int pos, int len)
	{
		try 
		{
			return this.mIs.read(((byte[]) (bytes.b) ), ((int) (pos) ), ((int) (len) ));
		}
		catch (java.io.IOException e)
		{
			throw haxe.lang.HaxeException.wrap(( "IOException: " + haxe.root.Std.string(e) ));
		}
		
		
	}
	
	
	public  java.io.InputStream mIs;
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef67 = true;
			switch (field.hashCode())
			{
				case 107127:
				{
					if (field.equals("mIs")) 
					{
						__temp_executeDef67 = false;
						this.mIs = ((java.io.InputStream) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef67) 
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
			boolean __temp_executeDef68 = true;
			switch (field.hashCode())
			{
				case 107127:
				{
					if (field.equals("mIs")) 
					{
						__temp_executeDef68 = false;
						return this.mIs;
					}
					
					break;
				}
				
				
				case -1140063115:
				{
					if (field.equals("readBytes")) 
					{
						__temp_executeDef68 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("readBytes"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef68) 
			{
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		baseArr.push("mIs");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


