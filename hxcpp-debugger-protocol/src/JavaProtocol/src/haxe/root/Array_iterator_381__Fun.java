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
public  class Array_iterator_381__Fun<T> extends haxe.lang.Function
{
	public    Array_iterator_381__Fun(haxe.root.Array<java.lang.Object> i, haxe.root.Array<haxe.root.Array> _g)
	{
		super(0, 0);
		this.i = i;
		this._g = _g;
	}
	
	
	@Override public   java.lang.Object __hx_invoke0_o()
	{
		T[] __temp_stmt54 = ((haxe.root.Array<T>) (((haxe.root.Array) (this._g.__get(0)) )) ).__a;
		int __temp_stmt55 = 0;
		{
			int __temp_arrIndex40 = 0;
			int __temp_arrVal38 = ((int) (haxe.lang.Runtime.toInt(this.i.__get(__temp_arrIndex40))) );
			int __temp_arrRet39 = __temp_arrVal38++;
			int __temp_expr56 = ((int) (haxe.lang.Runtime.toInt(this.i.__set(__temp_arrIndex40, __temp_arrVal38))) );
			__temp_stmt55 = __temp_arrRet39;
		}
		
		return __temp_stmt54[__temp_stmt55];
	}
	
	
	public  haxe.root.Array<java.lang.Object> i;
	
	public  haxe.root.Array<haxe.root.Array> _g;
	
}


