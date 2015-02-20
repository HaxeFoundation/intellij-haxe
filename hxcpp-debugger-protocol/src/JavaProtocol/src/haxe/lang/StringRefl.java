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
public  class StringRefl
{
	static 
	{
		haxe.lang.StringRefl.fields = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"length", "toUpperCase", "toLowerCase", "charAt", "charCodeAt", "indexOf", "lastIndexOf", "split", "substr", "substring"});
	}
	public    StringRefl()
	{
		{
		}
		
	}
	
	
	public static  haxe.root.Array<java.lang.String> fields;
	
	public static   java.lang.Object handleGetField(java.lang.String str, java.lang.String f, boolean throwErrors)
	{
		{
			java.lang.String __temp_svar202 = (f);
			int __temp_hash204 = __temp_svar202.hashCode();
			boolean __temp_executeDef203 = true;
			switch (__temp_hash204)
			{
				case -1106363674:
				{
					if (__temp_svar202.equals("length")) 
					{
						__temp_executeDef203 = false;
						return str.length();
					}
					
					break;
				}
				
				
				case 530542161:case -891529231:case 109648666:case -467511597:case 1943291465:case 397153782:case -1361633751:case -1137582698:case -399551817:
				{
					if (( (( ( __temp_hash204 == 530542161 ) && __temp_svar202.equals("substring") )) || ( (( ( __temp_hash204 == -891529231 ) && __temp_svar202.equals("substr") )) || ( (( ( __temp_hash204 == 109648666 ) && __temp_svar202.equals("split") )) || ( (( ( __temp_hash204 == -467511597 ) && __temp_svar202.equals("lastIndexOf") )) || ( (( ( __temp_hash204 == 1943291465 ) && __temp_svar202.equals("indexOf") )) || ( (( ( __temp_hash204 == 397153782 ) && __temp_svar202.equals("charCodeAt") )) || ( (( ( __temp_hash204 == -1361633751 ) && __temp_svar202.equals("charAt") )) || ( (( ( __temp_hash204 == -1137582698 ) && __temp_svar202.equals("toLowerCase") )) || __temp_svar202.equals("toUpperCase") ) ) ) ) ) ) ) )) 
					{
						__temp_executeDef203 = false;
						return new haxe.lang.Closure(((java.lang.Object) (str) ), haxe.lang.Runtime.toString(f));
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef203) 
			{
				if (throwErrors) 
				{
					throw haxe.lang.HaxeException.wrap(( ( "Field not found: \'" + f ) + "\' in String" ));
				}
				 else 
				{
					return null;
				}
				
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	public static   java.lang.Object handleCallField(java.lang.String str, java.lang.String f, haxe.root.Array args)
	{
		haxe.root.Array _args = new haxe.root.Array(new java.lang.Object[]{str});
		if (( args == null )) 
		{
			args = _args;
		}
		 else 
		{
			args = _args.concat(args);
		}
		
		return haxe.lang.Runtime.slowCallField(haxe.lang.StringExt.class, f, args);
	}
	
	
}


