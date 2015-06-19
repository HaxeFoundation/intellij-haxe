class Test1 extends Test2 {
  override public function a(<error descr="Type String is not compatible with Int">a:String</error>) { }
  override public function b(<error descr="Unexpected argument">a:String</error>) { }
  override public function <error descr="Not matching arity expected 1 arguments but found 0">c</error>() { }
}

class Test2 {
  public function a(a:Int) { }
  public function b() { }
  public function c(b:Int) { }
}