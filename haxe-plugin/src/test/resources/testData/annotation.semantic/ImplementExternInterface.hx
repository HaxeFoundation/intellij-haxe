class Test1 implements ExternInterfaceTest {
  public function fooExtern(b:Int):Int return 2;
}

extern interface ExternInterfaceTest {
  function fooExtern(b:Int):Int;
}
