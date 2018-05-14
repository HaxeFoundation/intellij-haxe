class Test1 extends Test2 {
  override public function a(<error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Int</b></td></tr><tr><td>Found:</td><td>String</td></tr></table></body></html>">a:String</error>) { }
  override public function b(<error descr="Unexpected parameter">a:String</error>) { }
  override public function <error descr="Not matching arity, expected 1 parameters count, got 0">c</error>() { }
}

class Test2 {
  public function a(a:Int) { }
  public function b() { }
  public function c(b:Int) { }
}