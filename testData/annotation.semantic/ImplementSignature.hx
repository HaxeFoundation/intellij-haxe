class Test1 implements Test3 {
  public function test(a:Int) return 10;
  public function <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>String</b></td></tr><tr><td>Found:</td><td>Int</td></tr></table></body></html>">test2</error>(b:Int) return 10;
  public function test3(b:Int) return 'test';
  public function test4(b:Int, <error descr="Unexpected parameter">c:String</error>)<error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Int</b></td></tr><tr><td>Found:</td><td>String</td></tr></table></body></html>">:String</error> {
    return 'test';
  }
}

interface Test3 {
  public function test(b:Int):Int;
  public function test2(b:Int):String;
  public function test3(b:Int):String;
  public function test4(b:Int):Int;
}
