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
public  class Array_iterator_380__Fun extends haxe.lang.Function
{
	public    Array_iterator_380__Fun(haxe.root.Array<java.lang.Object> len, haxe.root.Array<java.lang.Object> i)
	{
		super(0, 0);
		this.len = len;
		this.i = i;
	}
	
	
	@Override public   java.lang.Object __hx_invoke0_o()
	{
		return ( ((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) ) < ((int) (haxe.lang.Runtime.toInt(this.len.__get(0))) ) );
	}
	
	
	public  haxe.root.Array<java.lang.Object> len;
	
	public  haxe.root.Array<java.lang.Object> i;
	
}


