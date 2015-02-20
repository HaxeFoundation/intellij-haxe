package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class FieldLookup extends haxe.lang.HxObject
{
	public    FieldLookup(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    FieldLookup()
	{
		haxe.lang.FieldLookup.__hx_ctor_haxe_lang_FieldLookup(this);
	}
	
	
	public static   void __hx_ctor_haxe_lang_FieldLookup(haxe.lang.FieldLookup __temp_me31)
	{
		{
		}
		
	}
	
	
	public static   int hash(java.lang.String s)
	{
		
		return s.hashCode();
	
	}
	
	
	public static   int findHash(java.lang.String hash, haxe.root.Array<java.lang.String> hashs)
	{
		int min = 0;
		int max = hashs.length;
		while (( min < max ))
		{
			int mid = ( (( max + min )) / 2 );
			int classify = hash.compareTo(hashs.__get(mid));
			if (( classify < 0 )) 
			{
				max = mid;
			}
			 else 
			{
				if (( classify > 0 )) 
				{
					min = ( mid + 1 );
				}
				 else 
				{
					return mid;
				}
				
			}
			
		}
		
		return  ~ (min) ;
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.lang.FieldLookup(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.lang.FieldLookup();
	}
	
	
}


