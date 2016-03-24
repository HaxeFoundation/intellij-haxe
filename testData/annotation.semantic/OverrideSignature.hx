class Test1 extends Test2 {
  override public function test1(a:Int, <error descr="Unexpected argument">b:St<caret>ring</error>) {

  }
}

class Test2 {
  public function test1(a:Int) {

  }
}