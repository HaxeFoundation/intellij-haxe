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
package haxe.ds;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class IntMap_keys_343__Fun<T> extends haxe.lang.Function
{
	public    IntMap_keys_343__Fun(haxe.root.Array<java.lang.Object> i, haxe.root.Array<haxe.ds.IntMap> _g1)
	{
		super(0, 1);
		this.i = i;
		this._g1 = _g1;
	}
	
	
	@Override public   double __hx_invoke0_f()
	{
		int ret = ((haxe.ds.IntMap<T>) (((haxe.ds.IntMap) (this._g1.__get(0)) )) )._keys[((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) )];
		((haxe.ds.IntMap<T>) (((haxe.ds.IntMap) (this._g1.__get(0)) )) ).cachedIndex = ((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) );
		((haxe.ds.IntMap<T>) (((haxe.ds.IntMap) (this._g1.__get(0)) )) ).cachedKey = ret;
		this.i.__set(0, ( ((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) ) + 1 ));
		return ((double) (ret) );
	}
	
	
	public  haxe.root.Array<java.lang.Object> i;
	
	public  haxe.root.Array<haxe.ds.IntMap> _g1;
	
}


