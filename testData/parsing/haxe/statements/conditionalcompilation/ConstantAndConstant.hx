package;
class Test {
 #if (cpp && js)  // Test sets only one of these at a time.
 function bar() {}
 #end
}