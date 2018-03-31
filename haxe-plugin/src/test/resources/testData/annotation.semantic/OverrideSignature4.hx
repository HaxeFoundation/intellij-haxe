class Test1 extends Test2 {
  override public function test1(<error descr="Unexpected argument">a:I<caret>nt</error>, <error descr="Unexpected argument">?b:String</error>, <error descr="Unexpected argument">c:Int</error>) {

  }
}

class Test2 {
  public function test1() {

  }
}