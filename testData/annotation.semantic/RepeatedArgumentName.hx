class Test {
  function test(<warning descr="Duplicate parameter name 'a'">a</warning>:Int, b:Int, <warning descr="Duplicate parameter name 'a'">a</warning>:Int) { }
}