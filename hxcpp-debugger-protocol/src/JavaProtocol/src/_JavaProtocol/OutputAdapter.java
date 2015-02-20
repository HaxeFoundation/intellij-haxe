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
public  class OutputAdapter extends haxe.io.Output
{
	public    OutputAdapter(haxe.lang.EmptyObject empty)
	{
		super(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public    OutputAdapter(java.io.OutputStream os)
	{
		_JavaProtocol.OutputAdapter.__hx_ctor__JavaProtocol_OutputAdapter(this, os);
	}
	
	
	public static   void __hx_ctor__JavaProtocol_OutputAdapter(_JavaProtocol.OutputAdapter __temp_me14, java.io.OutputStream os)
	{
		__temp_me14.mOs = os;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new _JavaProtocol.OutputAdapter(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (arr.__get(0)) ));
	}
	
	
	@Override public   int writeBytes(haxe.io.Bytes bytes, int pos, int len)
	{
		try 
		{
			this.mOs.write(((byte[]) (bytes.b) ), ((int) (pos) ), ((int) (len) ));
			return len;
		}
		catch (java.io.IOException e)
		{
			throw haxe.lang.HaxeException.wrap(( "IOException: " + haxe.root.Std.string(e) ));
		}
		
		
	}
	
	
	public  java.io.OutputStream mOs;
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef63 = true;
			switch (field.hashCode())
			{
				case 107313:
				{
					if (field.equals("mOs")) 
					{
						__temp_executeDef63 = false;
						this.mOs = ((java.io.OutputStream) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef63) 
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
			boolean __temp_executeDef64 = true;
			switch (field.hashCode())
			{
				case 107313:
				{
					if (field.equals("mOs")) 
					{
						__temp_executeDef64 = false;
						return this.mOs;
					}
					
					break;
				}
				
				
				case -662729780:
				{
					if (field.equals("writeBytes")) 
					{
						__temp_executeDef64 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeBytes"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef64) 
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
		baseArr.push("mOs");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


