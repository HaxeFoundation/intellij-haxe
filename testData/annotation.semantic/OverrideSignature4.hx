class Test1 extends Test2 {
  override public function test1(<error descr="Unexpected parameter">a:<caret>Int</error>, <error descr="Unexpected parameter">?b:String</error>, <error descr="Unexpected parameter">c:Int</error>) {

  }
}

class Test2 {
  public function test1() {

  }
}