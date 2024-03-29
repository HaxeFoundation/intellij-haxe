// Generated by Haxe 4.3.0
package haxe.ds._IntMap;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public final class IntMapKeyIterator<T> extends haxe.lang.HxObject
{
	public IntMapKeyIterator(haxe.lang.EmptyObject empty)
	{
	}
	
	
	public IntMapKeyIterator(haxe.ds.IntMap<T> m)
	{
		//line 471 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		haxe.ds._IntMap.IntMapKeyIterator.__hx_ctor_haxe_ds__IntMap_IntMapKeyIterator(((haxe.ds._IntMap.IntMapKeyIterator<T>) (this) ), ((haxe.ds.IntMap<T>) (m) ));
		//line 471 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		java.lang.Object __temp_expr1 = ((java.lang.Object) (null) );
	}
	
	
	public static <T_c> void __hx_ctor_haxe_ds__IntMap_IntMapKeyIterator(haxe.ds._IntMap.IntMapKeyIterator<T_c> __hx_this, haxe.ds.IntMap<T_c> m)
	{
		//line 472 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		__hx_this.i = 0;
		//line 473 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		__hx_this.m = m;
		//line 474 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		__hx_this.len = m.nBuckets;
	}
	
	
	public haxe.ds.IntMap<T> m;
	
	public int i;
	
	public int len;
	
	public final boolean hasNext()
	{
		//line 478 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 478 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			int _g = this.i;
			//line 478 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			int _g1 = this.len;
			//line 478 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			while (( _g < _g1 ))
			{
				//line 478 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				int j = _g++;
				//line 479 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				if (( (( ( this.m.flags[( j >> 4 )] >>> (( (( j & 15 )) << 1 )) ) & 3 )) == 0 )) 
				{
					//line 480 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
					this.i = j;
					//line 481 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
					return true;
				}
				
			}
			
		}
		
		//line 484 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		return false;
	}
	
	
	public final int next()
	{
		//line 488 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		int ret = this.m._keys[this.i];
		//line 490 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		this.m.cachedIndex = this.i;
		//line 491 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		this.m.cachedKey = ret;
		//line 493 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		this.i++;
		//line 494 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		return ret;
	}
	
	
	@Override public double __hx_setField_f(java.lang.String field, double value, boolean handleProperties)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			boolean __temp_executeDef1 = true;
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (( field != null )) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				switch (field.hashCode())
				{
					case 107029:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("len")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							this.len = ((int) (value) );
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return value;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 105:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("i")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							this.i = ((int) (value) );
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return value;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
				}
				
			}
			
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (__temp_executeDef1) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				return super.__hx_setField_f(field, value, handleProperties);
			}
			else
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				throw null;
			}
			
		}
		
	}
	
	
	@Override public java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			boolean __temp_executeDef1 = true;
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (( field != null )) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				switch (field.hashCode())
				{
					case 107029:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("len")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							this.len = ((int) (haxe.lang.Runtime.toInt(value)) );
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return value;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 109:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("m")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							this.m = ((haxe.ds.IntMap<T>) (value) );
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return value;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 105:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("i")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							this.i = ((int) (haxe.lang.Runtime.toInt(value)) );
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return value;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
				}
				
			}
			
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (__temp_executeDef1) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				return super.__hx_setField(field, value, handleProperties);
			}
			else
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				throw null;
			}
			
		}
		
	}
	
	
	@Override public java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			boolean __temp_executeDef1 = true;
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (( field != null )) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				switch (field.hashCode())
				{
					case 3377907:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("next")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return ((haxe.lang.Function) (new haxe.lang.Closure(this, "next")) );
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 109:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("m")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return this.m;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 696759469:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("hasNext")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return ((haxe.lang.Function) (new haxe.lang.Closure(this, "hasNext")) );
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 105:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("i")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return this.i;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 107029:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("len")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return this.len;
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
				}
				
			}
			
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (__temp_executeDef1) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
			}
			else
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				throw null;
			}
			
		}
		
	}
	
	
	@Override public double __hx_getField_f(java.lang.String field, boolean throwErrors, boolean handleProperties)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			boolean __temp_executeDef1 = true;
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (( field != null )) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				switch (field.hashCode())
				{
					case 107029:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("len")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return ((double) (this.len) );
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 105:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("i")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return ((double) (this.i) );
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
				}
				
			}
			
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (__temp_executeDef1) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				return super.__hx_getField_f(field, throwErrors, handleProperties);
			}
			else
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				throw null;
			}
			
		}
		
	}
	
	
	@Override public java.lang.Object __hx_invokeField(java.lang.String field, java.lang.Object[] dynargs)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		{
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			boolean __temp_executeDef1 = true;
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (( field != null )) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				switch (field.hashCode())
				{
					case 3377907:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("next")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return this.next();
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
					case 696759469:
					{
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						if (field.equals("hasNext")) 
						{
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							__temp_executeDef1 = false;
							//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
							return this.hasNext();
						}
						
						//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
						break;
					}
					
					
				}
				
			}
			
			//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
			if (__temp_executeDef1) 
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				return super.__hx_invokeField(field, dynargs);
			}
			else
			{
				//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
				throw null;
			}
			
		}
		
	}
	
	
	@Override public void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		baseArr.push("len");
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		baseArr.push("i");
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		baseArr.push("m");
		//line 466 "C:\\HaxeToolkit\\haxe\\std\\java\\_std\\haxe\\ds\\IntMap.hx"
		super.__hx_getFields(baseArr);
	}
	
	
}


