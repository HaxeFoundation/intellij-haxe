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
public  class Date extends haxe.lang.HxObject
{
	public    Date(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    Date(int year, int month, int day, java.lang.Object hour, java.lang.Object min, java.lang.Object sec, java.lang.Object millisec)
	{
		haxe.root.Date.__hx_ctor__Date(this, year, month, day, hour, min, sec, millisec);
	}
	
	
	public static   void __hx_ctor__Date(haxe.root.Date __temp_me11, int year, int month, int day, java.lang.Object hour, java.lang.Object min, java.lang.Object sec, java.lang.Object millisec)
	{
		int __temp_millisec10 = ( (( millisec == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(millisec)) )) );
		int __temp_sec9 = ( (( sec == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(sec)) )) );
		int __temp_min8 = ( (( min == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(min)) )) );
		int __temp_hour7 = ( (( hour == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(hour)) )) );
		if (( year != 0 )) 
		{
			year = ( year - 1900 );
		}
		 else 
		{
			year = 0;
		}
		
		__temp_me11.date = new java.util.Date(((int) (year) ), ((int) (month) ), ((int) (day) ), ((int) (__temp_hour7) ), ((int) (__temp_min8) ), ((int) (__temp_sec9) ));
		if (( __temp_millisec10 > 0 )) 
		{
			long __temp_stmt60 = 0L;
			{
				long a = __temp_me11.date.getTime();
				long b = 0L;
				{
					long i = 0L;
					i = ((long) (( ( ((long) (0) ) << 32 ) | ( __temp_millisec10 & 0xffffffffL ) )) );
					b = i;
				}
				
				__temp_stmt60 = ((long) (( ((long) (a) ) + ((long) (b) ) )) );
			}
			
			__temp_me11.date = new java.util.Date(((long) (__temp_stmt60) ));
		}
		
	}
	
	
	public static   haxe.root.Date fromUTC(int year, int month, int day, java.lang.Object hour, java.lang.Object min, java.lang.Object sec, java.lang.Object millisec)
	{
		int __temp_millisec5 = ( (( millisec == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(millisec)) )) );
		int __temp_sec4 = ( (( sec == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(sec)) )) );
		int __temp_min3 = ( (( min == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(min)) )) );
		int __temp_hour2 = ( (( hour == null )) ? (((int) (0) )) : (((int) (haxe.lang.Runtime.toInt(hour)) )) );
		haxe.root.Date d = new haxe.root.Date(((int) (year) ), ((int) (month) ), ((int) (day) ), ((java.lang.Object) (__temp_hour2) ), ((java.lang.Object) (__temp_min3) ), ((java.lang.Object) (__temp_sec4) ), ((java.lang.Object) (__temp_millisec5) ));
		return haxe.root.Date.fromTime(( (( ( ((double) (d.date.getTime()) ) / 1000 ) + d.timezoneOffset() )) * 1000 ));
	}
	
	
	public static   haxe.root.Date fromTime(double t)
	{
		haxe.root.Date d = new haxe.root.Date(((int) (0) ), ((int) (0) ), ((int) (0) ), ((java.lang.Object) (0) ), ((java.lang.Object) (0) ), ((java.lang.Object) (0) ), ((java.lang.Object) (null) ));
		d.date = new java.util.Date(((long) (t) ));
		return d;
	}
	
	
	public static   haxe.root.Date fromString(java.lang.String s, java.lang.Object isUtc)
	{
		boolean __temp_isUtc6 = ( (( isUtc == null )) ? (haxe.lang.Runtime.toBool(false)) : (haxe.lang.Runtime.toBool(isUtc)) );
		int _g = s.length();
		switch (_g)
		{
			case 8:
			{
				haxe.root.Array<java.lang.String> k = haxe.lang.StringExt.split(s, ":");
				if (__temp_isUtc6) 
				{
					return haxe.root.Date.fromUTC(0, 0, 1, haxe.root.Std.parseInt(k.__get(0)), haxe.root.Std.parseInt(k.__get(1)), haxe.root.Std.parseInt(k.__get(2)), null);
				}
				 else 
				{
					return new haxe.root.Date(((int) (0) ), ((int) (0) ), ((int) (1) ), ((java.lang.Object) (haxe.root.Std.parseInt(k.__get(0))) ), ((java.lang.Object) (haxe.root.Std.parseInt(k.__get(1))) ), ((java.lang.Object) (haxe.root.Std.parseInt(k.__get(2))) ), ((java.lang.Object) (null) ));
				}
				
			}
			
			
			case 10:
			{
				haxe.root.Array<java.lang.String> k = haxe.lang.StringExt.split(s, "-");
				if (__temp_isUtc6) 
				{
					return haxe.root.Date.fromUTC(((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(0)))) ), ( ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(1)))) ) - ((int) (1) ) ), ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(2)))) ), null, null, null, null);
				}
				 else 
				{
					return new haxe.root.Date(((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(0)))) ), ((int) (( ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(1)))) ) - ((int) (1) ) )) ), ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(k.__get(2)))) ), ((java.lang.Object) (null) ), ((java.lang.Object) (null) ), ((java.lang.Object) (null) ), ((java.lang.Object) (null) ));
				}
				
			}
			
			
			case 19:
			{
				haxe.root.Array<java.lang.String> k = haxe.lang.StringExt.split(s, " ");
				haxe.root.Array<java.lang.String> y = haxe.lang.StringExt.split(k.__get(0), "-");
				haxe.root.Array<java.lang.String> t = haxe.lang.StringExt.split(k.__get(1), ":");
				if (__temp_isUtc6) 
				{
					return haxe.root.Date.fromUTC(((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(0)))) ), ( ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(1)))) ) - ((int) (1) ) ), ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(2)))) ), haxe.root.Std.parseInt(t.__get(0)), haxe.root.Std.parseInt(t.__get(1)), haxe.root.Std.parseInt(t.__get(2)), null);
				}
				 else 
				{
					return new haxe.root.Date(((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(0)))) ), ((int) (( ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(1)))) ) - ((int) (1) ) )) ), ((int) (haxe.lang.Runtime.toInt(haxe.root.Std.parseInt(y.__get(2)))) ), ((java.lang.Object) (haxe.root.Std.parseInt(t.__get(0))) ), ((java.lang.Object) (haxe.root.Std.parseInt(t.__get(1))) ), ((java.lang.Object) (haxe.root.Std.parseInt(t.__get(2))) ), ((java.lang.Object) (null) ));
				}
				
			}
			
			
			default:
			{
				throw haxe.lang.HaxeException.wrap(( "Invalid date format : " + s ));
			}
			
		}
		
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.Date(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.Date(((int) (haxe.lang.Runtime.toInt(arr.__get(0))) ), ((int) (haxe.lang.Runtime.toInt(arr.__get(1))) ), ((int) (haxe.lang.Runtime.toInt(arr.__get(2))) ), ((java.lang.Object) (arr.__get(3)) ), ((java.lang.Object) (arr.__get(4)) ), ((java.lang.Object) (arr.__get(5)) ), ((java.lang.Object) (arr.__get(6)) ));
	}
	
	
	public  java.util.Date date;
	
	public   int timezoneOffset()
	{
		return this.date.getTimezoneOffset();
	}
	
	
	@Override public   java.lang.String toString()
	{
		int m = ( this.date.getMonth() + 1 );
		int d = this.date.getDate();
		int h = this.date.getHours();
		int mi = this.date.getMinutes();
		int s = this.date.getSeconds();
		return ( ( ( ( ( ( ( ( ( ( ( this.date.getYear() + 1900 ) + "-" ) + (( (( m < 10 )) ? (( "0" + m )) : (( "" + m )) )) ) + "-" ) + (( (( d < 10 )) ? (( "0" + d )) : (( "" + d )) )) ) + " " ) + (( (( h < 10 )) ? (( "0" + h )) : (( "" + h )) )) ) + ":" ) + (( (( mi < 10 )) ? (( "0" + mi )) : (( "" + mi )) )) ) + ":" ) + (( (( s < 10 )) ? (( "0" + s )) : (( "" + s )) )) );
	}
	
	
	@Override public   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties)
	{
		{
			boolean __temp_executeDef57 = true;
			switch (field.hashCode())
			{
				case 3076014:
				{
					if (field.equals("date")) 
					{
						__temp_executeDef57 = false;
						this.date = ((java.util.Date) (value) );
						return value;
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef57) 
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
			boolean __temp_executeDef58 = true;
			switch (field.hashCode())
			{
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef58 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("toString"))) );
					}
					
					break;
				}
				
				
				case 3076014:
				{
					if (field.equals("date")) 
					{
						__temp_executeDef58 = false;
						return this.date;
					}
					
					break;
				}
				
				
				case -201721364:
				{
					if (field.equals("timezoneOffset")) 
					{
						__temp_executeDef58 = false;
						return ((haxe.lang.Function) (new haxe.lang.Closure(((java.lang.Object) (this) ), haxe.lang.Runtime.toString("timezoneOffset"))) );
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef58) 
			{
				return super.__hx_getField(field, throwErrors, isCheck, handleProperties);
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
			boolean __temp_executeDef59 = true;
			switch (field.hashCode())
			{
				case -1776922004:
				{
					if (field.equals("toString")) 
					{
						__temp_executeDef59 = false;
						return this.toString();
					}
					
					break;
				}
				
				
				case -201721364:
				{
					if (field.equals("timezoneOffset")) 
					{
						__temp_executeDef59 = false;
						return this.timezoneOffset();
					}
					
					break;
				}
				
				
			}
			
			if (__temp_executeDef59) 
			{
				return super.__hx_invokeField(field, dynargs);
			}
			 else 
			{
				throw null;
			}
			
		}
		
	}
	
	
	@Override public   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr)
	{
		baseArr.push("date");
		{
			super.__hx_getFields(baseArr);
		}
		
	}
	
	
}


