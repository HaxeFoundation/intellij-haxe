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
public  class Sys extends haxe.lang.HxObject
{
	public    Sys(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Sys()
	{
		haxe.root.Sys.__hx_ctor__Sys(this);
	}
	
	
	public static   void __hx_ctor__Sys(haxe.root.Sys __temp_me21)
	{
		{
		}
		
	}
	
	
	public static   haxe.io.Output stderr()
	{
		return new haxe.java.io.NativeOutput(((java.io.OutputStream) (java.lang.System.err) ));
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.Sys(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.Sys();
	}
	
	
}


