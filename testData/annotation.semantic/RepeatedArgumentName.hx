class Test {
  function test(<warning descr="Repeated argument name 'a'">a</warning>:Int, b:Int, <warning descr="Repeated argument name 'a'">a</warning>:Int) { }
}