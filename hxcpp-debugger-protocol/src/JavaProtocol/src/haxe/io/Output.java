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
package haxe.io;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Output extends haxe.lang.HxObject
{
	public    Output(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Output()
	{
		haxe.io.Output.__hx_ctor_haxe_io_Output(this);
	}
	
	
	public static   void __hx_ctor_haxe_io_Output(haxe.io.Output __temp_me13)
	{
		{
		}
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.io.Output(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.io.Output();
	}
	
	
	public   void writeByte(int c)
	{
		throw haxe.lang.HaxeException.wrap("Not implemented");
	}
	
	
	public   int writeBytes(haxe.io.Bytes s, int pos, int len)
	{
		int k = len;
		byte[] b = s.b;
		if (( ( ( pos < 0 ) || ( len < 0 ) ) || ( ( pos + len ) > s.length ) )) 
		{
			throw haxe.lang.HaxeException.wrap(haxe.io.Error.OutsideBounds);
		}
		
		while (( k > 0 ))
		{
			this.writeByte(((int) (b[pos]) ));
			pos++;
			k--;
		}
		
		return len;
	}
	
	
	public   void write(haxe.io.Bytes s)
	{
		int l = s.length;
		int p = 0;
		while (( l > 0 ))
		{
			int k = this.writeBytes(s, p, l);
			if (( k == 0 )) 
			{
				throw haxe.lang.HaxeException.wrap(haxe.io.Error.Blocked);
			}
			
			p += k;
			l -= k;
		}
		
	}
	
	
	public   void writeFullBytes(haxe.io.Bytes s, int pos, int len)
	{
		while (( len > 0 ))
		{
			int k = this.writeBytes(s, pos, len);
			pos += k;
			len -= k;
		}
		
	}
	
	
	public   void writeString(java.lang.String s)
	{
		haxe.io.Bytes b = haxe.io.Bytes.ofString(s);
		this.writeFullBytes(b, 0, b.length);
	}
	
	
	@Override public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			boolean __temp_executeDef61 = true;
			switch (field.hashCode())
			{
				case 1412235472:
				{
					if (field.equals("writeString")) 
					{
						__temp_executeDef61 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeString"))) );
					}
					
					break;
				}
				
				
				case -1406851705:
				{
					if (field.equals("writeByte")) 
					{
						__temp_executeDef61 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeByte"))) );
					}
					
					break;
				}
				
				
				case 1188045309:
				{
					if (field.equals("writeFullBytes")) 
					{
						__temp_executeDef61 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeFullBytes"))) );
					}
					
					break;
				}
				
				
				case -662729780:
				{
					if (field.equals("writeBytes")) 
					{
						__temp_executeDef61 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("writeBytes"))) );
					}
					
					break;
				}
				
				
				case 113399775:
				{
					if (field.equals("write")) 
					{
						__temp_executeDef61 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("write"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef61) 
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
			boolean __temp_executeDef62 = true;
			switch (field.hashCode())
			{
				case 1412235472:
				{
					if (field.equals("writeString")) 
					{
						__temp_executeDef62 = false;
						this.writeString(haxe.lang.Runtime.toString(dynargs.__get(0)));
					}
					
					break;
				}
				
				
				case -1406851705:
				{
					if (field.equals("writeByte")) 
					{
						__temp_executeDef62 = false;
						this.writeByte(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ));
					}
					
					break;
				}
				
				
				case 1188045309:
				{
					if (field.equals("writeFullBytes")) 
					{
						__temp_executeDef62 = false;
						this.writeFullBytes(((haxe.io.Bytes) (dynargs.__get(0)) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(2))) ));
					}
					
					break;
				}
				
				
				case -662729780:
				{
					if (field.equals("writeBytes")) 
					{
						__temp_executeDef62 = false;
						return this.writeBytes(((haxe.io.Bytes) (dynargs.__get(0)) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(2))) ));
					}
					
					break;
				}
				
				
				case 113399775:
				{
					if (field.equals("write")) 
					{
						__temp_executeDef62 = false;
						this.write(((haxe.io.Bytes) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef62) 
			{
				return super.__hx_invokeField(field, dynargs);
			}
			
		}
		
		return null;
	}
	
	
}


