package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class StringExt
{
	public    StringExt()
	{
		{
		}
		
	}
	
	
	public static   java.lang.String charAt(java.lang.String me, int index)
	{
		
			if ( index >= me.length() || index < 0 )
				return "";
			else
				return java.lang.Character.toString(me.charAt(index));
	
	}
	
	
	public static   java.lang.Object charCodeAt(java.lang.String me, int index)
	{
		
			if ( index >= me.length() || index < 0 )
				return null;
			else
				return me.codePointAt(index);
	
	}
	
	
	public static   int indexOf(java.lang.String me, java.lang.String str, java.lang.Object startIndex)
	{
		
			int sIndex = (startIndex != null ) ? (haxe.lang.Runtime.toInt(startIndex)) : 0;
			if (sIndex >= me.length() || sIndex < 0)
				return -1;
			return me.indexOf(str, sIndex);
	
	}
	
	
	public static   int lastIndexOf(java.lang.String me, java.lang.String str, java.lang.Object startIndex)
	{
		
			int sIndex = (startIndex != null ) ? (haxe.lang.Runtime.toInt(startIndex)) : (me.length() - 1);
			if (sIndex > me.length() || sIndex < 0)
				sIndex = me.length() - 1;
			else if (sIndex < 0)
				return -1;
			return me.lastIndexOf(str, sIndex);
	
	}
	
	
	public static   haxe.root.Array<java.lang.String> split(java.lang.String me, java.lang.String delimiter)
	{
		
			Array<java.lang.String> ret = new Array<java.lang.String>();

			int slen = delimiter.length();
			if (slen == 0)
			{
				int len = me.length();
				for (int i = 0; i < len; i++)
				{
					ret.push(me.substring(i, i + 1));
				}
			} else {
				int start = 0;
				int pos = me.indexOf(delimiter, start);

				while (pos >= 0)
				{
					ret.push(me.substring(start, pos));

					start = pos + slen;
					pos = me.indexOf(delimiter, start);
				}

				ret.push(me.substring(start));
			}
			return ret;
	
	}
	
	
	public static   java.lang.String substr(java.lang.String me, int pos, java.lang.Object len)
	{
		
			int meLen = me.length();
			int targetLen = meLen;
			if (len != null)
			{
				targetLen = haxe.lang.Runtime.toInt(len);
				if (targetLen == 0)
					return "";
				if( pos != 0 && targetLen < 0 ){
					return "";
				}
			}

			if( pos < 0 ){
				pos = meLen + pos;
				if( pos < 0 ) pos = 0;
			} else if( targetLen < 0 ){
				targetLen = meLen + targetLen - pos;
			}

			if( pos + targetLen > meLen ){
				targetLen = meLen - pos;
			}

			if ( pos < 0 || targetLen <= 0 ) return "";

			return me.substring(pos, pos + targetLen);
	
	}
	
	
	public static   java.lang.String substring(java.lang.String me, int startIndex, java.lang.Object endIndex)
	{
		
		int endIdx;
		int len = me.length();
		if ( endIndex == null) {
			endIdx = len;
		} else if ( (endIdx = haxe.lang.Runtime.toInt(endIndex)) < 0 ) {
			endIdx = 0;
		} else if ( endIdx > len ) {
			endIdx = len;
		}

		if ( startIndex < 0 ) {
			startIndex = 0;
		} else if ( startIndex > len ) {
			startIndex = len;
		}

		if ( startIndex > endIdx ) {
			int tmp = startIndex;
			startIndex = endIdx;
			endIdx = tmp;
		}

		return me.substring(startIndex, endIdx);

	
	}
	
	
	public static   java.lang.String toLowerCase(java.lang.String me)
	{
		
			return me.toLowerCase();
	
	}
	
	
	public static   java.lang.String toUpperCase(java.lang.String me)
	{
		
			return me.toUpperCase();
	
	}
	
	
	public static   java.lang.String toNativeString(java.lang.String me)
	{
		return me;
	}
	
	
	public static   java.lang.String fromCharCode(int code)
	{
		
		return java.lang.Character.toString( (char) code );
	
	}
	
	
}


