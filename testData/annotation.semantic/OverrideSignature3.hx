class Test1 extends Test2 {
  override public function test1(a:Int, <error descr="Unexpected parameter">?b:<caret>String</error>, <error descr="Unexpected parameter">c:Int</error>) {

  }
}

class Test2 {
  public function test1(a:Int) {

  }
}