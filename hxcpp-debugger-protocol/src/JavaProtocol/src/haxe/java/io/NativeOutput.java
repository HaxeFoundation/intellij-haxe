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
package haxe.java.io;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class NativeOutput extends haxe.io.Output
{
	public    NativeOutput(haxe.lang.EmptyObject empty)
	{
		super(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public    NativeOutput(java.io.OutputStream stream)
	{
		haxe.java.io.NativeOutput.__hx_ctor_haxe_java_io_NativeOutput(this, stream);
	}
	
	
	public static   void __hx_ctor_haxe_java_io_NativeOutput(haxe.java.io.NativeOutput __temp_me37, java.io.OutputStream stream)
	{
		__temp_me37.stream = stream;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.java.io.NativeOutput(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.java.io.NativeOutput(((java.io.OutputStream) (arr.__get(0)) ));
	}
	
	
	public  java.io.OutputStream stream;
	
	@Override public   void writeByte(int c)
	{
		try 
		{
			this.stream.write(((int) (c) ));
		}
		catch (java.io.EOFException e)
		{
			throw haxe.lang.HaxeException.wrap(new haxe.io.Eof());
		}
		
		catch (java.io.IOException e)
		{
			throw haxe.lang.HaxeException.wrap(haxe.io.Error.Custom(e));
		}
		
		
	}
	
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef205 = true;
			switch (field.hashCode())
			{
				case -891990144:
				{
					if (field.equals("stream")) 
					{
						__temp_executeDef205 = false;
						this.stream = ((java.io.OutputStream) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef205) 
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
			boolean __temp_executeDef206 = true;
			switch (field.hashCode())
			{
				case -1406851705:
				{
					if (field.equals("writeByte")) 
					{
						__temp_executeDef206 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeByte"))) );
					}
					
					break;
				}
				
				
				case -891990144:
				{
					if (field.equals("stream")) 
					{
						__temp_executeDef206 = false;
						return this.stream;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef206) 
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
		baseArr.push("stream");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


