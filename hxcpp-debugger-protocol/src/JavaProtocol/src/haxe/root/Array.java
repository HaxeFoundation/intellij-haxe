package haxe.root;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public final class Array<T> extends haxe.lang.HxObject
{
	
	public Array(T[] _native)
	{
		this.__a = _native;
		this.length = _native.length;
	}
	public    Array(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Array()
	{
		haxe.root.Array.__hx_ctor__Array(this);
	}
	
	
	public static  <T_c> void __hx_ctor__Array(haxe.root.Array<T_c> __temp_me1)
	{
		__temp_me1.length = 0;
		__temp_me1.__a = ((T_c[]) (((java.lang.Object) (new java.lang.Object[((int) (0) )]) )) );
	}
	
	
	public static  <X> haxe.root.Array<X> ofNative(X[] _native)
	{
		
			return new Array<X>(_native);
	
	}
	
	
	public static  <Y> haxe.root.Array<Y> alloc(int size)
	{
		
			return new Array<Y>((Y[]) ((java.lang.Object)new java.lang.Object[size]));
	
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.Array<java.lang.Object>(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.Array<java.lang.Object>();
	}
	
	
	public  int length;
	
	public  T[] __a;
	
	public final   haxe.root.Array<T> concat(haxe.root.Array<T> a)
	{
		int length = this.length;
		int len = ( length + a.length );
		T[] retarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (len) )]) )) );
		java.lang.System.arraycopy(((java.lang.Object) (this.__a) ), ((int) (0) ), ((java.lang.Object) (retarr) ), ((int) (0) ), ((int) (length) ));
		java.lang.System.arraycopy(((java.lang.Object) (a.__a) ), ((int) (0) ), ((java.lang.Object) (retarr) ), ((int) (length) ), ((int) (a.length) ));
		return haxe.root.Array.ofNative(retarr);
	}
	
	
	public final   void concatNative(T[] a)
	{
		T[] __a = this.__a;
		int length = this.length;
		int len = ( length + a.length );
		if (( __a.length >= len )) 
		{
			java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (0) ), ((java.lang.Object) (__a) ), ((int) (length) ), ((int) (length) ));
		}
		 else 
		{
			T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (len) )]) )) );
			java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (0) ), ((int) (length) ));
			java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (length) ), ((int) (a.length) ));
			this.__a = newarr;
		}
		
		this.length = len;
	}
	
	
	public final   java.lang.String join(java.lang.String sep)
	{
		haxe.root.StringBuf buf = new haxe.root.StringBuf();
		int i = -1;
		boolean first = true;
		int length = this.length;
		while ((  ++ i < length ))
		{
			if (first) 
			{
				first = false;
			}
			 else 
			{
				buf.add(sep);
			}
			
			buf.add(this.__a[i]);
		}
		
		return buf.toString();
	}
	
	
	public final   T pop()
	{
		T[] __a = this.__a;
		int length = this.length;
		if (( length > 0 )) 
		{
			T val = __a[ -- length];
			__a[length] = null;
			this.length = length;
			return val;
		}
		 else 
		{
			return null;
		}
		
	}
	
	
	public final   int push(T x)
	{
		int length = this.length;
		if (( length >= this.__a.length )) 
		{
			int newLen = ( (( length << 1 )) + 1 );
			T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (newLen) )]) )) );
			java.lang.System.arraycopy(((java.lang.Object) (this.__a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (0) ), ((int) (this.__a.length) ));
			this.__a = newarr;
		}
		
		this.__a[length] = x;
		return  ++ this.length;
	}
	
	
	public final   void reverse()
	{
		int i = 0;
		int l = this.length;
		T[] a = this.__a;
		int half = ( l >> 1 );
		l -= 1;
		while (( i < half ))
		{
			T tmp = a[i];
			a[i] = a[( l - i )];
			a[( l - i )] = tmp;
			i += 1;
		}
		
	}
	
	
	public final   T shift()
	{
		int l = this.length;
		if (( l == 0 )) 
		{
			return null;
		}
		
		T[] a = this.__a;
		T x = a[0];
		l -= 1;
		java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (1) ), ((java.lang.Object) (a) ), ((int) (0) ), ((int) (( this.length - 1 )) ));
		a[l] = null;
		this.length = l;
		return x;
	}
	
	
	public final   haxe.root.Array<T> slice(int pos, java.lang.Object end)
	{
		if (( pos < 0 )) 
		{
			pos = ( this.length + pos );
			if (( pos < 0 )) 
			{
				pos = 0;
			}
			
		}
		
		if (( end == null )) 
		{
			end = this.length;
		}
		 else 
		{
			if (( haxe.lang.Runtime.compare(end, 0) < 0 )) 
			{
				end = ((int) (haxe.lang.Runtime.toInt(haxe.lang.Runtime.plus(this.length, end))) );
			}
			
		}
		
		if (( haxe.lang.Runtime.compare(end, this.length) > 0 )) 
		{
			end = this.length;
		}
		
		int len = ( ((int) (haxe.lang.Runtime.toInt(end)) ) - ((int) (pos) ) );
		if (( len < 0 )) 
		{
			return new haxe.root.Array<T>();
		}
		
		T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (len) )]) )) );
		java.lang.System.arraycopy(((java.lang.Object) (this.__a) ), ((int) (pos) ), ((java.lang.Object) (newarr) ), ((int) (0) ), ((int) (len) ));
		return haxe.root.Array.ofNative(newarr);
	}
	
	
	public final   void sort(haxe.lang.Function f)
	{
		if (( this.length == 0 )) 
		{
			return ;
		}
		
		this.quicksort(0, ( this.length - 1 ), f);
	}
	
	
	public final   void quicksort(int lo, int hi, haxe.lang.Function f)
	{
		T[] buf = this.__a;
		int i = lo;
		int j = hi;
		T p = buf[( ( i + j ) >> 1 )];
		while (( i <= j ))
		{
			while (( ((int) (f.__hx_invoke2_f(0.0, 0.0, buf[i], p)) ) < 0 ))
			{
				i++;
			}
			
			while (( ((int) (f.__hx_invoke2_f(0.0, 0.0, buf[j], p)) ) > 0 ))
			{
				j--;
			}
			
			if (( i <= j )) 
			{
				T t = buf[i];
				buf[i++] = buf[j];
				buf[j--] = t;
			}
			
		}
		
		if (( lo < j )) 
		{
			this.quicksort(lo, j, f);
		}
		
		if (( i < hi )) 
		{
			this.quicksort(i, hi, f);
		}
		
	}
	
	
	public final   haxe.root.Array<T> splice(int pos, int len)
	{
		if (( len < 0 )) 
		{
			return new haxe.root.Array<T>();
		}
		
		if (( pos < 0 )) 
		{
			pos = ( this.length + pos );
			if (( pos < 0 )) 
			{
				pos = 0;
			}
			
		}
		
		if (( pos > this.length )) 
		{
			pos = 0;
			len = 0;
		}
		 else 
		{
			if (( ( pos + len ) > this.length )) 
			{
				len = ( this.length - pos );
				if (( len < 0 )) 
				{
					len = 0;
				}
				
			}
			
		}
		
		T[] a = this.__a;
		T[] ret = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (len) )]) )) );
		java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (pos) ), ((java.lang.Object) (ret) ), ((int) (0) ), ((int) (len) ));
		haxe.root.Array<T> ret1 = haxe.root.Array.ofNative(ret);
		int end = ( pos + len );
		java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (end) ), ((java.lang.Object) (a) ), ((int) (pos) ), ((int) (( this.length - end )) ));
		this.length -= len;
		while ((  -- len >= 0 ))
		{
			a[( this.length + len )] = null;
		}
		
		return ret1;
	}
	
	
	public final   void spliceVoid(int pos, int len)
	{
		if (( len < 0 )) 
		{
			return ;
		}
		
		if (( pos < 0 )) 
		{
			pos = ( this.length + pos );
			if (( pos < 0 )) 
			{
				pos = 0;
			}
			
		}
		
		if (( pos > this.length )) 
		{
			pos = 0;
			len = 0;
		}
		 else 
		{
			if (( ( pos + len ) > this.length )) 
			{
				len = ( this.length - pos );
				if (( len < 0 )) 
				{
					len = 0;
				}
				
			}
			
		}
		
		T[] a = this.__a;
		int end = ( pos + len );
		java.lang.System.arraycopy(((java.lang.Object) (a) ), ((int) (end) ), ((java.lang.Object) (a) ), ((int) (pos) ), ((int) (( this.length - end )) ));
		this.length -= len;
		while ((  -- len >= 0 ))
		{
			a[( this.length + len )] = null;
		}
		
	}
	
	
	@Override public   java.lang.String toString()
	{
		haxe.root.StringBuf ret = new haxe.root.StringBuf();
		T[] a = this.__a;
		ret.add("[");
		boolean first = true;
		{
			int _g1 = 0;
			int _g = this.length;
			while (( _g1 < _g ))
			{
				int i = _g1++;
				if (first) 
				{
					first = false;
				}
				 else 
				{
					ret.add(",");
				}
				
				ret.add(a[i]);
			}
			
		}
		
		ret.add("]");
		return ret.toString();
	}
	
	
	public final   void unshift(T x)
	{
		T[] __a = this.__a;
		int length = this.length;
		if (( length >= __a.length )) 
		{
			int newLen = ( (( length << 1 )) + 1 );
			T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (newLen) )]) )) );
			java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (1) ), ((int) (length) ));
			this.__a = newarr;
		}
		 else 
		{
			java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (__a) ), ((int) (1) ), ((int) (length) ));
		}
		
		this.__a[0] = x;
		 ++ this.length;
	}
	
	
	public final   void insert(int pos, T x)
	{
		int l = this.length;
		if (( pos < 0 )) 
		{
			pos = ( l + pos );
			if (( pos < 0 )) 
			{
				pos = 0;
			}
			
		}
		
		if (( pos >= l )) 
		{
			this.push(x);
			return ;
		}
		 else 
		{
			if (( pos == 0 )) 
			{
				this.unshift(x);
				return ;
			}
			
		}
		
		if (( l >= this.__a.length )) 
		{
			int newLen = ( (( this.length << 1 )) + 1 );
			T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (newLen) )]) )) );
			java.lang.System.arraycopy(((java.lang.Object) (this.__a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (0) ), ((int) (pos) ));
			newarr[pos] = x;
			java.lang.System.arraycopy(((java.lang.Object) (this.__a) ), ((int) (pos) ), ((java.lang.Object) (newarr) ), ((int) (( pos + 1 )) ), ((int) (( l - pos )) ));
			this.__a = newarr;
			 ++ this.length;
		}
		 else 
		{
			T[] __a = this.__a;
			java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (pos) ), ((java.lang.Object) (__a) ), ((int) (( pos + 1 )) ), ((int) (( l - pos )) ));
			java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (__a) ), ((int) (0) ), ((int) (pos) ));
			__a[pos] = x;
			 ++ this.length;
		}
		
	}
	
	
	public final   boolean remove(T x)
	{
		T[] __a = this.__a;
		int i = -1;
		int length = this.length;
		while ((  ++ i < length ))
		{
			if (haxe.lang.Runtime.eq(__a[i], x)) 
			{
				java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (( i + 1 )) ), ((java.lang.Object) (__a) ), ((int) (i) ), ((int) (( ( length - i ) - 1 )) ));
				__a[ -- this.length] = null;
				return true;
			}
			
		}
		
		return false;
	}
	
	
	public final   haxe.root.Array<T> copy()
	{
		int len = this.length;
		T[] __a = this.__a;
		T[] newarr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (len) )]) )) );
		java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (newarr) ), ((int) (0) ), ((int) (len) ));
		return haxe.root.Array.ofNative(newarr);
	}
	
	
	public final   java.lang.Object iterator()
	{
		haxe.root.Array<haxe.root.Array> _g = new haxe.root.Array<haxe.root.Array>(new haxe.root.Array[]{((haxe.root.Array) (this) )});
		haxe.root.Array<java.lang.Object> i = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{0});
		haxe.root.Array<java.lang.Object> len = new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{this.length});
		{
			haxe.lang.Function __temp_odecl47 = new haxe.root.Array_iterator_380__Fun(((haxe.root.Array<java.lang.Object>) (len) ), ((haxe.root.Array<java.lang.Object>) (i) ));
			haxe.lang.Function __temp_odecl48 = new haxe.root.Array_iterator_381__Fun<T>(((haxe.root.Array<java.lang.Object>) (i) ), ((haxe.root.Array<haxe.root.Array>) (_g) ));
			return new haxe.lang.DynamicObject(new haxe.root.Array<java.lang.String>(new java.lang.String[]{"hasNext", "next"}), new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{__temp_odecl47, __temp_odecl48}), new haxe.root.Array<java.lang.String>(new java.lang.String[]{}), new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{}));
		}
		
	}
	
	
	public final  <S> haxe.root.Array<S> map(haxe.lang.Function f)
	{
		haxe.root.Array<S> ret = new haxe.root.Array<S>(( (S[]) (new java.lang.Object[] {}) ));
		{
			int _g = 0;
			haxe.root.Array<T> _g1 = this;
			while (( _g < _g1.length ))
			{
				T elt = _g1.__get(_g);
				 ++ _g;
				ret.push(((S) (f.__hx_invoke1_o(0.0, elt)) ));
			}
			
		}
		
		return ret;
	}
	
	
	public final   haxe.root.Array<T> filter(haxe.lang.Function f)
	{
		haxe.root.Array<T> ret = new haxe.root.Array<T>(( (T[]) (new java.lang.Object[] {}) ));
		{
			int _g = 0;
			haxe.root.Array<T> _g1 = this;
			while (( _g < _g1.length ))
			{
				T elt = _g1.__get(_g);
				 ++ _g;
				if (haxe.lang.Runtime.toBool(f.__hx_invoke1_o(0.0, elt))) 
				{
					ret.push(elt);
				}
				
			}
			
		}
		
		return ret;
	}
	
	
	public final   T __get(int idx)
	{
		T[] __a = this.__a;
		if (( ( idx >= __a.length ) || ( idx < 0 ) )) 
		{
			return null;
		}
		
		return __a[idx];
	}
	
	
	public final   T __set(int idx, T v)
	{
		T[] __a = this.__a;
		if (( idx >= __a.length )) 
		{
			int newl = ( idx + 1 );
			if (( idx == __a.length )) 
			{
				newl = ( (( idx << 1 )) + 1 );
			}
			
			T[] newArr = ((T[]) (((java.lang.Object) (new java.lang.Object[((int) (newl) )]) )) );
			if (( this.length > 0 )) 
			{
				java.lang.System.arraycopy(((java.lang.Object) (__a) ), ((int) (0) ), ((java.lang.Object) (newArr) ), ((int) (0) ), ((int) (this.length) ));
			}
			
			this.__a = __a = newArr;
		}
		
		if (( idx >= this.length )) 
		{
			this.length = ( idx + 1 );
		}
		
		return __a[idx] = v;
	}
	
	
	public final   T __unsafe_get(int idx)
	{
		return this.__a[idx];
	}
	
	
	public final   T __unsafe_set(int idx, T val)
	{
		return this.__a[idx] = val;
	}
	
	
	@Override public   double __hx_setField_f(java.lang.String field, double value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef49 = true;
			switch (field.hashCode())
			{
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef49 = false;
						this.length = ((int) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef49) 
			{
				return super.__hx_setField_f(field, value, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef50 = true;
			switch (field.hashCode())
			{
				case 94337:
				{
					if (field.equals("__a")) 
					{
						__temp_executeDef50 = false;
						this.__a = ((T[]) (value) );
						return value;
					}
					
					break;
				}
				
				
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef50 = false;
						this.length = ((int) (haxe.lang.Runtime.toInt(value)) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef50) 
			{
				return super.__hx_setField(field, value, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties)
	{
		{
			boolean __temp_executeDef51 = true;
			switch (field.hashCode())
			{
				case -537840087:
				{
					if (field.equals("__unsafe_set")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("__unsafe_set"))) );
					}
					
					break;
				}
				
				
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef51 = false;
						return this.length;
					}
					
					break;
				}
				
				
				case -537851619:
				{
					if (field.equals("__unsafe_get")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("__unsafe_get"))) );
					}
					
					break;
				}
				
				
				case 94337:
				{
					if (field.equals("__a")) 
					{
						__temp_executeDef51 = false;
						return this.__a;
					}
					
					break;
				}
				
				
				case 90678402:
				{
					if (field.equals("__set")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("__set"))) );
					}
					
					break;
				}
				
				
				case -1354795244:
				{
					if (field.equals("concat")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("concat"))) );
					}
					
					break;
				}
				
				
				case 90666870:
				{
					if (field.equals("__get")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("__get"))) );
					}
					
					break;
				}
				
				
				case -1238494133:
				{
					if (field.equals("concatNative")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("concatNative"))) );
					}
					
					break;
				}
				
				
				case -1274492040:
				{
					if (field.equals("filter")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("filter"))) );
					}
					
					break;
				}
				
				
				case 3267882:
				{
					if (field.equals("join")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("join"))) );
					}
					
					break;
				}
				
				
				case 107868:
				{
					if (field.equals("map")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("map"))) );
					}
					
					break;
				}
				
				
				case 111185:
				{
					if (field.equals("pop")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("pop"))) );
					}
					
					break;
				}
				
				
				case 1182533742:
				{
					if (field.equals("iterator")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("iterator"))) );
					}
					
					break;
				}
				
				
				case 3452698:
				{
					if (field.equals("push")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("push"))) );
					}
					
					break;
				}
				
				
				case 3059573:
				{
					if (field.equals("copy")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("copy"))) );
					}
					
					break;
				}
				
				
				case 1099846370:
				{
					if (field.equals("reverse")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("reverse"))) );
					}
					
					break;
				}
				
				
				case -934610812:
				{
					if (field.equals("remove")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("remove"))) );
					}
					
					break;
				}
				
				
				case 109407362:
				{
					if (field.equals("shift")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("shift"))) );
					}
					
					break;
				}
				
				
				case -1183792455:
				{
					if (field.equals("insert")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("insert"))) );
					}
					
					break;
				}
				
				
				case 109526418:
				{
					if (field.equals("slice")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("slice"))) );
					}
					
					break;
				}
				
				
				case -277637751:
				{
					if (field.equals("unshift")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("unshift"))) );
					}
					
					break;
				}
				
				
				case 3536286:
				{
					if (field.equals("sort")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("sort"))) );
					}
					
					break;
				}
				
				
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("toString"))) );
					}
					
					break;
				}
				
				
				case 1301699851:
				{
					if (field.equals("quicksort")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("quicksort"))) );
					}
					
					break;
				}
				
				
				case -821858768:
				{
					if (field.equals("spliceVoid")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("spliceVoid"))) );
					}
					
					break;
				}
				
				
				case -895859076:
				{
					if (field.equals("splice")) 
					{
						__temp_executeDef51 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("splice"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef51) 
			{
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   double __hx_getField_f(java.lang.String field, boolean throwErrors, boolean handleProperties)
	{
		{
			boolean __temp_executeDef52 = true;
			switch (field.hashCode())
			{
				case -1106363674:
				{
					if (field.equals("length")) 
					{
						__temp_executeDef52 = false;
						return ((double) (this.length) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef52) 
			{
				return super.__hx_getField_f(field, throwErrors, handleProperties);
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
			boolean __temp_executeDef53 = true;
			switch (field.hashCode())
			{
				case -537840087:
				{
					if (field.equals("__unsafe_set")) 
					{
						__temp_executeDef53 = false;
						return this.__unsafe_set(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((T) (dynargs.__get(1)) ));
					}
					
					break;
				}
				
				
				case -1354795244:
				{
					if (field.equals("concat")) 
					{
						__temp_executeDef53 = false;
						return this.concat(((haxe.root.Array<T>) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case -537851619:
				{
					if (field.equals("__unsafe_get")) 
					{
						__temp_executeDef53 = false;
						return this.__unsafe_get(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ));
					}
					
					break;
				}
				
				
				case -1238494133:
				{
					if (field.equals("concatNative")) 
					{
						__temp_executeDef53 = false;
						this.concatNative(((T[]) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case 90678402:
				{
					if (field.equals("__set")) 
					{
						__temp_executeDef53 = false;
						return this.__set(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((T) (dynargs.__get(1)) ));
					}
					
					break;
				}
				
				
				case 3267882:
				{
					if (field.equals("join")) 
					{
						__temp_executeDef53 = false;
						return this.join(haxe.lang.Runtime.toString(dynargs.__get(0)));
					}
					
					break;
				}
				
				
				case 90666870:
				{
					if (field.equals("__get")) 
					{
						__temp_executeDef53 = false;
						return this.__get(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ));
					}
					
					break;
				}
				
				
				case 111185:
				{
					if (field.equals("pop")) 
					{
						__temp_executeDef53 = false;
						return this.pop();
					}
					
					break;
				}
				
				
				case -1274492040:
				{
					if (field.equals("filter")) 
					{
						__temp_executeDef53 = false;
						return this.filter(((haxe.lang.Function) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case 3452698:
				{
					if (field.equals("push")) 
					{
						__temp_executeDef53 = false;
						return this.push(((T) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case 107868:
				{
					if (field.equals("map")) 
					{
						__temp_executeDef53 = false;
						return this.map(((haxe.lang.Function) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case 1099846370:
				{
					if (field.equals("reverse")) 
					{
						__temp_executeDef53 = false;
						this.reverse();
					}
					
					break;
				}
				
				
				case 1182533742:
				{
					if (field.equals("iterator")) 
					{
						__temp_executeDef53 = false;
						return this.iterator();
					}
					
					break;
				}
				
				
				case 109407362:
				{
					if (field.equals("shift")) 
					{
						__temp_executeDef53 = false;
						return this.shift();
					}
					
					break;
				}
				
				
				case 3059573:
				{
					if (field.equals("copy")) 
					{
						__temp_executeDef53 = false;
						return this.copy();
					}
					
					break;
				}
				
				
				case 109526418:
				{
					if (field.equals("slice")) 
					{
						__temp_executeDef53 = false;
						return this.slice(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), dynargs.__get(1));
					}
					
					break;
				}
				
				
				case -934610812:
				{
					if (field.equals("remove")) 
					{
						__temp_executeDef53 = false;
						return this.remove(((T) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case 3536286:
				{
					if (field.equals("sort")) 
					{
						__temp_executeDef53 = false;
						this.sort(((haxe.lang.Function) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case -1183792455:
				{
					if (field.equals("insert")) 
					{
						__temp_executeDef53 = false;
						this.insert(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((T) (dynargs.__get(1)) ));
					}
					
					break;
				}
				
				
				case 1301699851:
				{
					if (field.equals("quicksort")) 
					{
						__temp_executeDef53 = false;
						this.quicksort(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ), ((haxe.lang.Function) (dynargs.__get(2)) ));
					}
					
					break;
				}
				
				
				case -277637751:
				{
					if (field.equals("unshift")) 
					{
						__temp_executeDef53 = false;
						this.unshift(((T) (dynargs.__get(0)) ));
					}
					
					break;
				}
				
				
				case -895859076:
				{
					if (field.equals("splice")) 
					{
						__temp_executeDef53 = false;
						return this.splice(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ));
					}
					
					break;
				}
				
				
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef53 = false;
						return this.toString();
					}
					
					break;
				}
				
				
				case -821858768:
				{
					if (field.equals("spliceVoid")) 
					{
						__temp_executeDef53 = false;
						this.spliceVoid(((int) (haxe.lang.Runtime.toInt(dynargs.__get(0))) ), ((int) (haxe.lang.Runtime.toInt(dynargs.__get(1))) ));
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef53) 
			{
				return super.__hx_invokeField(field, dynargs);
			}
			
		}
		
		return null;
	}
	
	
	@Override public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		baseArr.push("__a");
		baseArr.push("length");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


