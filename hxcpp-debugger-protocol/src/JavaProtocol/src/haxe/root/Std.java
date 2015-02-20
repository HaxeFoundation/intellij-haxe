/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package haxe.root;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Std
{
	public    Std()
	{
		{
		}
		
	}
	
	
	public static   boolean is(java.lang.Object v, java.lang.Object t)
	{
		if (( v == null )) 
		{
			return haxe.lang.Runtime.eq(t, java.lang.Object.class);
		}
		
		if (( t == null )) 
		{
			return false;
		}
		
		java.lang.Class clt = ((java.lang.Class) (t) );
		if (( ((java.lang.Object) (clt) ) == ((java.lang.Object) (null) ) )) 
		{
			return false;
		}
		
		java.lang.String name = clt.getName();
		{
			java.lang.String __temp_svar77 = (name);
			int __temp_hash79 = __temp_svar77.hashCode();
			switch (__temp_hash79)
			{
				case 761287205:case -1325958191:
				{
					if (( (( ( __temp_hash79 == 761287205 ) && __temp_svar77.equals("java.lang.Double") )) || __temp_svar77.equals("double") )) 
					{
						return haxe.lang.Runtime.isDouble(v);
					}
					
					break;
				}
				
				
				case 1063877011:
				{
					if (__temp_svar77.equals("java.lang.Object")) 
					{
						return true;
					}
					
					break;
				}
				
				
				case -2056817302:case 104431:
				{
					if (( (( ( __temp_hash79 == -2056817302 ) && __temp_svar77.equals("java.lang.Integer") )) || __temp_svar77.equals("int") )) 
					{
						return haxe.lang.Runtime.isInt(v);
					}
					
					break;
				}
				
				
				case 344809556:case 64711720:
				{
					if (( (( ( __temp_hash79 == 344809556 ) && __temp_svar77.equals("java.lang.Boolean") )) || __temp_svar77.equals("boolean") )) 
					{
						return v instanceof java.lang.Boolean;
					}
					
					break;
				}
				
				
			}
			
		}
		
		java.lang.Class clv = v.getClass();
		return clt.isAssignableFrom(((java.lang.Class) (clv) ));
	}
	
	
	public static   java.lang.String string(java.lang.Object s)
	{
		return ( haxe.lang.Runtime.toString(s) + "" );
	}
	
	
	public static   java.lang.Object parseInt(java.lang.String x)
	{
		
		if (x == null) return null;

		int ret = 0;
		int base = 10;
		int i = 0;
		int len = x.length();

		if (x.startsWith("0") && len > 2)
		{
			char c = x.charAt(1);
			if (c == 'x' || c == 'X')
			{
				i = 2;
				base = 16;
			}
		}

		boolean foundAny = false;
		boolean isNeg = false;
		for (; i < len; i++)
		{
			char c = x.charAt(i);
			if (!foundAny)
			{
				switch(c)
				{
					case '-':
						isNeg = true;
						continue;
          case '+':
					case '\n':
					case '\t':
					case '\r':
					case ' ':
						if (isNeg) return null;
						continue;
				}
			}

			if (c >= '0' && c <= '9')
			{
				if (!foundAny && c == '0')
				{
					foundAny = true;
					continue;
				}
				ret *= base; foundAny = true;

				ret += ((int) (c - '0'));
			} else if (base == 16) {
				if (c >= 'a' && c <= 'f') {
					ret *= base; foundAny = true;
					ret += ((int) (c - 'a')) + 10;
				} else if (c >= 'A' && c <= 'F') {
					ret *= base; foundAny = true;
					ret += ((int) (c - 'A')) + 10;
				} else {
					break;
				}
			} else {
				break;
			}
		}

		if (foundAny)
			return isNeg ? -ret : ret;
		else
			return null;
	
	}
	
	
	public static   double parseFloat(java.lang.String x)
	{
		
		if (x == null) return java.lang.Double.NaN;

		x = x.trim();
		double ret = 0.0;
		double div = 0.0;
		double e = 0.0;

		int len = x.length();
		boolean foundAny = false;
		boolean isNeg = false;
		for (int i = 0; i < len; i++)
		{
			char c = x.charAt(i);
			if (!foundAny)
			{
				switch(c)
				{
					case '-':
						isNeg = true;
						continue;
          case '+':
					case '\n':
					case '\t':
					case '\r':
					case ' ':
					if (isNeg) return java.lang.Double.NaN;
						continue;
				}
			}

			if (c == '.') {
				if (div != 0.0)
					break;
				div = 1.0;

				continue;
			}

			if (c >= '0' && c <= '9')
			{
				if (!foundAny && c == '0')
				{
					foundAny = true;
					continue;
				}
				ret *= 10.0; foundAny = true; div *= 10.0;

				ret += ((int) (c - '0'));
			} else if (foundAny && c == 'E' || c == 'e') {
				boolean eNeg = false;
				boolean eFoundAny = false;

				char next = x.charAt(i + 1);
				if (i + 1 < len)
				{
					if (next == '-')
					{
						eNeg = true;
						i++;
					} else if (next == '+') {
						i++;
					}
				}

				while (++i < len)
				{
					c = x.charAt(i);
					if (c >= '0' && c <= '9')
					{
						if (!eFoundAny && c == '0')
							continue;
						eFoundAny = true;
						e *= 10.0;
						e += ((int) (c - '0'));
					} else {
						break;
					}
				}

				if (eNeg) e = -e;
			} else {
				break;
			}
		}

		if (div == 0.0) div = 1.0;

		if (foundAny)
		{
			ret = isNeg ? -(ret / div) : (ret / div);
			if (e != 0.0)
			{
				return ret * Math.pow(10.0, e);
			} else {
				return ret;
			}
		} else {
			return java.lang.Double.NaN;
		}
	
	}
	
	
}


