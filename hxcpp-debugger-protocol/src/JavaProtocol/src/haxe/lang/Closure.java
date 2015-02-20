package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Closure extends haxe.lang.VarArgsBase
{
	public    Closure(java.lang.Object obj, java.lang.String field)
	{
		super(-1, -1);
		this.obj = obj;
		this.field = field;
	}
	
	
	public  java.lang.Object obj;
	
	public  java.lang.String field;
	
	@Override public   java.lang.Object __hx_invokeDynamic(haxe.root.Array dynArgs)
	{
		return haxe.lang.Runtime.callField(this.obj, this.field, dynArgs);
	}
	
	
}


