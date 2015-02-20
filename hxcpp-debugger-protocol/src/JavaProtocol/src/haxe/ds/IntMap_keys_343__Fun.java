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


