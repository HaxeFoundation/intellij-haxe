package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class VarArgsFunction extends haxe.lang.VarArgsBase
{
	public    VarArgsFunction(haxe.lang.Function fun)
	{
		super(-1, -1);
		this.fun = fun;
	}
	
	
	public  haxe.lang.Function fun;
	
	@Override public   java.lang.Object __hx_invokeDynamic(haxe.root.Array dynArgs)
	{
		return ((java.lang.Object) (this.fun.__hx_invoke1_o(0.0, dynArgs)) );
	}
	
	
}


