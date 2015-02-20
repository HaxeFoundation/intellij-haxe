package haxe.lang;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  interface IHxObject
{
	   boolean __hx_deleteField(java.lang.String field);
	
	   java.lang.Object __hx_lookupField(java.lang.String field, boolean throwErrors, boolean isCheck);
	
	   double __hx_lookupField_f(java.lang.String field, boolean throwErrors);
	
	   java.lang.Object __hx_lookupSetField(java.lang.String field, java.lang.Object value);
	
	   double __hx_lookupSetField_f(java.lang.String field, double value);
	
	   double __hx_setField_f(java.lang.String field, double value, boolean handleProperties);
	
	   java.lang.Object __hx_setField(java.lang.String field, java.lang.Object value, boolean handleProperties);
	
	   java.lang.Object __hx_getField(java.lang.String field, boolean throwErrors, boolean isCheck, boolean handleProperties);
	
	   double __hx_getField_f(java.lang.String field, boolean throwErrors, boolean handleProperties);
	
	   java.lang.Object __hx_invokeField(java.lang.String field, haxe.root.Array dynargs);
	
	   void __hx_getFields(haxe.root.Array<java.lang.String> baseArr);
	
}


