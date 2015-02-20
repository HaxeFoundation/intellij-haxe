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
public  class StringTools extends haxe.lang.HxObject
{
	public    StringTools(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    StringTools()
	{
		haxe.root.StringTools.__hx_ctor__StringTools(this);
	}
	
	
	public static   void __hx_ctor__StringTools(haxe.root.StringTools __temp_me20)
	{
		{
		}
		
	}
	
	
	public static   java.lang.String urlEncode(java.lang.String s)
	{
		try 
		{
			return java.net.URLEncoder.encode(s, "UTF-8");
		}
		catch (java.lang.Throwable __temp_catchallException83)
		{
			java.lang.Object __temp_catchall84 = __temp_catchallException83;
			if (( __temp_catchall84 instanceof haxe.lang.HaxeException )) 
			{
				__temp_catchall84 = ((haxe.lang.HaxeException) (__temp_catchallException83) ).obj;
			}
			
			{
				java.lang.Object e = __temp_catchall84;
				throw haxe.lang.HaxeException.wrap(e);
			}
			
		}
		
		
	}
	
	
	public static   java.lang.String urlDecode(java.lang.String s)
	{
		try 
		{
			return java.net.URLDecoder.decode(s, "UTF-8");
		}
		catch (java.lang.Throwable __temp_catchallException85)
		{
			java.lang.Object __temp_catchall86 = __temp_catchallException85;
			if (( __temp_catchall86 instanceof haxe.lang.HaxeException )) 
			{
				__temp_catchall86 = ((haxe.lang.HaxeException) (__temp_catchallException85) ).obj;
			}
			
			{
				java.lang.Object e = __temp_catchall86;
				throw haxe.lang.HaxeException.wrap(e);
			}
			
		}
		
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.StringTools(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.StringTools();
	}
	
	
}


