package;
class Test {
 #if (  1 == 1
     && 1 != 0
     && 1 >  0
     && 0 <  1
     && 1 >= 0
     && 1 >= 1
     && 0 <= 1
     && 1 <= 1
     && (!!1) == (!0)
     )
 function bar() {}
 #end
 #if (  "str" == "str"
     && "str" != "mystr"
     && "str" >  "aa"
     && "str" <  "zz"
     && "str" >= "str"
     && "str" >= "aa"
     && "str" <= "zz"
     && "str" <= "str"
     && (!"str") == (!"zz")
     )
 function foo() {}
 #end
 #if ( true == true
     && false != true
     && false == false
     )
 function asdf() {}
 #end
 #if ( false || "false" || 0 ) // true because "false" is a non-empty string.
 function f() {}
 #end
}