package;
class Test {
 #if ( 1 == true )  // Boolean compared to any other type is always false.
 function bar() {}
 #end
}