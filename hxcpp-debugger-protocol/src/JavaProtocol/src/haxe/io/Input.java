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
public  class Input extends haxe.lang.HxObject
{
	public    Input(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Input()
	{
		haxe.io.Input.__hx_ctor_haxe_io_Input(this);
	}
	
	
	public static   void __hx_ctor_haxe_io_Input(haxe.io.Input __temp_me15)
	{
		{
		}
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.io.Input(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.io.Input();
	}
	
	
	public   int readByte()
	{
		throw haxe.lang.HaxeException.wrap("Not implemented");
	}
	
	
	public   int readBytes(haxe.io.Bytes s, int pos, int len)
	{
		int k = len;
		byte[] b = s.b;
		if (( ( ( pos < 0 ) || ( len < 0 ) ) || ( ( pos + len ) > s.length ) )) 
		{
			throw haxe.lang.HaxeException.wrap(haxe.io.Error.OutsideBounds);
		}
		
		while (( k > 0 ))
		{
			b[pos] = ((byte) (this.readByte()) );
			pos++;
			k--;
		}
		
		return len;
	}
	
	
	public   haxe.io.Bytes read(int nbytes)
	{
		haxe.io.Bytes s = haxe.io.Bytes.alloc(nbytes);
		int p = 0;
		while (( nbytes > 0 ))
		{
			int k = this.readBytes(s, p, nbytes);
			if (( k == 0 )) 
			{
				throw haxe.lang.HaxeException.wrap(haxe.io.Error.Blocked);
			}
			
			p += k;
			nbytes -= k;
		}
		
		return s;
	}
	
	
	@Override public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			boolean __temp_executeDef65 = true;
			switch (field.hashCode())
			{
				case 3496342:
				{
					if (field.equals("read")) 
					{
						__temp_executeDef65 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("read"))) );
					}
					
					break;
				}
				
				
				case -868060226:
				{
					if (field.equals("readByte")) 
					{
						__temp_executeDef65 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("readByte"))) );
					}
					
					break;
				}
				
				
				case -1140063115:
				{
					if (field.equals("readBytes")) 
					{
						__temp_executeDef65 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("readBytes"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef65) 
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
			boolean __temp_executeDef66 = true;
			switch (field.hashCode())
			{
				case 3496342:
				{
					if (field.equals("read")) 
					{
						__temp_executeDef66 = false;
						return this.read(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ));
					}
					
					break;
				}
				
				
				case -868060226:
				{
					if (field.equals("readByte")) 
					{
						__temp_executeDef66 = false;
						return this.readByte();
					}
					
					break;
				}
				
				
				case -1140063115:
				{
					if (field.equals("readBytes")) 
					{
						__temp_executeDef66 = false;
						return this.readBytes(((haxe.io.Bytes) (dynargs.__get(0)) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(2))) ));
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef66) 
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


