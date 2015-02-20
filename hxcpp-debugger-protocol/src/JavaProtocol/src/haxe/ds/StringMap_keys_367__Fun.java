package haxe.ds;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StringMap_keys_367__Fun<T> extends haxe.lang.Function
{
	public    StringMap_keys_367__Fun(haxe.root.Array<java.lang.Object> i, haxe.root.Array<haxe.ds.StringMap> _g1)
	{
		super(0, 0);
		this.i = i;
		this._g1 = _g1;
	}
	
	
	@Override public   java.lang.Object __hx_invoke0_o()
	{
		java.lang.String ret = ((haxe.ds.StringMap<T>) (((haxe.ds.StringMap) (this._g1.__get(0)) )) )._keys[((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) )];
		((haxe.ds.StringMap<T>) (((haxe.ds.StringMap) (this._g1.__get(0)) )) ).cachedIndex = ((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) );
		((haxe.ds.StringMap<T>) (((haxe.ds.StringMap) (this._g1.__get(0)) )) ).cachedKey = ret;
		this.i.__set(0, ( ((int) (haxe.lang.Runtime.toInt(this.i.__get(0))) ) + 1 ));
		return ret;
	}
	
	
	public  haxe.root.Array<java.lang.Object> i;
	
	public  haxe.root.Array<haxe.ds.StringMap> _g1;
	
}


