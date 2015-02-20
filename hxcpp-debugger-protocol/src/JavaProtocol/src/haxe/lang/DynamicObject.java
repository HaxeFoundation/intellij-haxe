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
public  class DynamicObject extends haxe.lang.HxObject
{
	public    DynamicObject(haxe.lang.EmptyObject empty)
	{
		super(haxe.lang.EmptyObject.EMPTY);
	}
	
	
	public    DynamicObject()
	{
		haxe.lang.DynamicObject.__hx_ctor_haxe_lang_DynamicObject(((haxe.lang.DynamicObject) (this) ));
	}
	
	
	public    DynamicObject(haxe.root.Array<java.lang.String> __hx_hashes, haxe.root.Array<java.lang.Object> __hx_dynamics, haxe.root.Array<java.lang.String> __hx_hashes_f, haxe.root.Array<java.lang.Object> __hx_dynamics_f)
	{
		haxe.lang.DynamicObject.__hx_ctor_haxe_lang_DynamicObject(((haxe.lang.DynamicObject) (this) ), ((haxe.root.Array<java.lang.String>) (__hx_hashes) ), ((haxe.root.Array<java.lang.Object>) (__hx_dynamics) ), ((haxe.root.Array<java.lang.String>) (__hx_hashes_f) ), ((haxe.root.Array<java.lang.Object>) (__hx_dynamics_f) ));
	}
	
	
	public static   void __hx_ctor_haxe_lang_DynamicObject(haxe.lang.DynamicObject __temp_me34)
	{
		__temp_me34.__hx_hashes = new haxe.root.Array<java.lang.String>(new java.lang.String[]{});
		__temp_me34.__hx_dynamics = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{});
		__temp_me34.__hx_hashes_f = new haxe.root.Array<java.lang.String>(new java.lang.String[]{});
		__temp_me34.__hx_dynamics_f = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{});
	}
	
	
	public static   void __hx_ctor_haxe_lang_DynamicObject(haxe.lang.DynamicObject __temp_me33, haxe.root.Array<java.lang.String> __hx_hashes, haxe.root.Array<java.lang.Object> __hx_dynamics, haxe.root.Array<java.lang.String> __hx_hashes_f, haxe.root.Array<java.lang.Object> __hx_dynamics_f)
	{
		__temp_me33.__hx_hashes = __hx_hashes;
		__temp_me33.__hx_dynamics = __hx_dynamics;
		__temp_me33.__hx_hashes_f = __hx_hashes_f;
		__temp_me33.__hx_dynamics_f = __hx_dynamics_f;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.lang.DynamicObject(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.lang.DynamicObject(((haxe.root.Array<java.lang.String>) (arr.__get(0)) ), ((haxe.root.Array<java.lang.Object>) (arr.__get(1)) ), ((haxe.root.Array<java.lang.String>) (arr.__get(2)) ), ((haxe.root.Array<java.lang.Object>) (arr.__get(3)) ));
	}
	
	
	@Override public   java.lang.String toString()
	{
		haxe.lang.Function ts = ((haxe.lang.Function) (haxe.lang.Runtime.getField(this, "toString", false)) );
		if (( ts != null )) 
		{
			return haxe.lang.Runtime.toString(ts.__hx_invoke0_o());
		}
		
		haxe.root.StringBuf ret = new haxe.root.StringBuf();
		ret.add("{");
		boolean first = true;
		{
			int _g = 0;
			haxe.root.Array<java.lang.String> _g1 = haxe.root.Reflect.fields(this);
			while (( _g < _g1.length ))
			{
				java.lang.String f = _g1.__get(_g);
				 ++ _g;
				if (first) 
				{
					first = false;
				}
				 else 
				{
					ret.add(",");
				}
				
				ret.add(" ");
				ret.add(f);
				ret.add(" : ");
				ret.add(haxe.root.Reflect.field(this, f));
			}
			
		}
		
		if ( ! (first) ) 
		{
			ret.add(" ");
		}
		
		ret.add("}");
		return ret.toString();
	}
	
	
	@Override public   boolean __hx_deleteField(java.lang.String field)
	{
		int res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes);
		if (( res >= 0 )) 
		{
			this.__hx_hashes.splice(res, 1);
			this.__hx_dynamics.splice(res, 1);
			return true;
		}
		 else 
		{
			res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes_f);
			if (( res >= 0 )) 
			{
				this.__hx_hashes_f.splice(res, 1);
				this.__hx_dynamics_f.splice(res, 1);
				return true;
			}
			
		}
		
		return false;
	}
	
	
	public  haxe.root.Array<java.lang.String> __hx_hashes = new haxe.root.Array<java.lang.String>(new java.lang.String[]{});
	
	public  haxe.root.Array<java.lang.Object> __hx_dynamics = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{});
	
	public  haxe.root.Array<java.lang.String> __hx_hashes_f = new haxe.root.Array<java.lang.String>(new java.lang.String[]{});
	
	public  haxe.root.Array<java.lang.Object> __hx_dynamics_f = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{});
	
	@Override public   java.lang.Object __hx_lookupField(java.lang.String field, boolean throwErrors, boolean isCheck)
	{
		int res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes);
		if (( res >= 0 )) 
		{
			return this.__hx_dynamics.__get(res);
		}
		 else 
		{
			res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes_f);
			if (( res >= 0 )) 
			{
				return ((double) (haxe.lang.Runtime.toDouble(this.__hx_dynamics_f.__get(res))) );
			}
			
		}
		
		if (isCheck) 
		{
			return haxe.lang.Runtime.undefined;
		}
		 else 
		{
			return null;
		}
		
	}
	
	
	@Override public   double __hx_lookupField_f(java.lang.String field, boolean throwErrors)
	{
		int res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes_f);
		if (( res >= 0 )) 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_dynamics_f.__get(res))) );
		}
		 else 
		{
			res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes);
			if (( res >= 0 )) 
			{
				return ((double) (haxe.lang.Runtime.toDouble(this.__hx_dynamics.__get(res))) );
			}
			
		}
		
		return 0.0;
	}
	
	
	@Override public   java.lang.Object __hx_lookupSetField(java.lang.String field, java.lang.Object value)
	{
		int res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes);
		if (( res >= 0 )) 
		{
			return this.__hx_dynamics.__set(res, value);
		}
		 else 
		{
			int res2 = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes_f);
			if (( res >= 0 )) 
			{
				this.__hx_hashes_f.splice(res2, 1);
				this.__hx_dynamics_f.splice(res2, 1);
			}
			
		}
		
		this.__hx_hashes.insert( ~ (res) , field);
		this.__hx_dynamics.insert( ~ (res) , value);
		return value;
	}
	
	
	@Override public   double __hx_lookupSetField_f(java.lang.String field, double value)
	{
		int res = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes_f);
		if (( res >= 0 )) 
		{
			return ((double) (haxe.lang.Runtime.toDouble(this.__hx_dynamics_f.__set(res, value))) );
		}
		 else 
		{
			int res2 = haxe.lang.FieldLookup.findHash(field, this.__hx_hashes);
			if (( res >= 0 )) 
			{
				this.__hx_hashes.splice(res2, 1);
				this.__hx_dynamics.splice(res2, 1);
			}
			
		}
		
		this.__hx_hashes_f.insert( ~ (res) , field);
		this.__hx_dynamics_f.insert( ~ (res) , value);
		return value;
	}
	
	
	@Override public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		{
			{
				java.lang.Object __temp_iterator45 = this.__hx_hashes.iterator();
				while (haxe.lang.Runtime.toBool(haxe.lang.Runtime.callField(__temp_iterator45, "hasNext", null)))
				{
					java.lang.String __temp_field36 = haxe.lang.Runtime.toString(haxe.lang.Runtime.callField(__temp_iterator45, "next", null));
					baseArr.push(__temp_field36);
				}
				
			}
			
			{
				java.lang.Object __temp_iterator46 = this.__hx_hashes_f.iterator();
				while (haxe.lang.Runtime.toBool(haxe.lang.Runtime.callField(__temp_iterator46, "hasNext", null)))
				{
					java.lang.String __temp_field35 = haxe.lang.Runtime.toString(haxe.lang.Runtime.callField(__temp_iterator46, "next", null));
					baseArr.push(__temp_field35);
				}
				
			}
			
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


