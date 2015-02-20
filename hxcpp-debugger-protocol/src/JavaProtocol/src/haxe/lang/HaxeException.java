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
public  class HaxeException extends java.lang.RuntimeException
{
	public    HaxeException(java.lang.Object obj, java.lang.String msg, java.lang.Throwable cause)
	{
		super(msg, cause);
		if (( obj instanceof haxe.lang.HaxeException )) 
		{
			haxe.lang.HaxeException _obj = ((haxe.lang.HaxeException) (obj) );
			obj = _obj.getObject();
		}
		
		this.obj = obj;
	}
	
	
	public static   java.lang.RuntimeException wrap(java.lang.Object obj)
	{
		if (( obj instanceof java.lang.RuntimeException )) 
		{
			return ((java.lang.RuntimeException) (obj) );
		}
		
		if (( obj instanceof java.lang.String )) 
		{
			return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(obj), ((java.lang.Throwable) (null) ));
		}
		 else 
		{
			if (( obj instanceof java.lang.Throwable )) 
			{
				return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(null), ((java.lang.Throwable) (obj) ));
			}
			
		}
		
		return new haxe.lang.HaxeException(((java.lang.Object) (obj) ), haxe.lang.Runtime.toString(null), ((java.lang.Throwable) (null) ));
	}
	
	
	public  java.lang.Object obj;
	
	public   java.lang.Object getObject()
	{
		return this.obj;
	}
	
	
	@Override public   java.lang.String toString()
	{
		return ( "Haxe Exception: " + haxe.root.Std.string(this.obj) );
	}
	
	
}


