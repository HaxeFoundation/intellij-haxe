class Test1 extends Base {
  override public function a(<error descr="Incompatible type: String should be Int">a:String</error>) { }
  override public function b(<error descr="Unexpected argument">a:String</error>) { }
  override public function <error descr="Not matching arity expected (expected 1 argument(s) but found 0)">c</error>() { }
}
class Test2 extends Test1 {
  override public function a(<error descr="Incompatible type: Float should be String">a:Float</error>) { }
  <error descr="Overriding nothing">override</error> public function x(a:Int) { }
  override public function c(<error descr="Unexpected argument">a:Int</error>) { }
}

class Base {
  public function a(a:Int) { }
  public function b() { }
  public function c(b:Float) { }
}