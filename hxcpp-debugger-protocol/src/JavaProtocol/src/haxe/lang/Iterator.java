package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  interface Iterator<T> extends haxe.lang.IHxObject
{
	   boolean hasNext();
	
	   T next();
	
}


