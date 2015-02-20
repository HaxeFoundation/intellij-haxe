package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  interface Iterable<T> extends haxe.lang.IHxObject
{
	   haxe.lang.Iterator<T> iterator();
	
}


