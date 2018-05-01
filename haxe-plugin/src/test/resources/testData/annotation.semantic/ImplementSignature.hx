class Test1 implements Test3 {
  public function test(a:Int) return 10;
  public function <error descr="Not compatible return type Int = 10 != String">test2</error>(b:Int) return 10;
  public function test3(b:Int) return 'test';
  public function test4(b:Int, <error descr="Unexpected argument">c:String</error>)<error descr="Not compatible return type String != Int">:String</error> {
    return 'test';
  }
}

interface Test3 {
  public function test(b:Int):Int;
  public function test2(b:Int):String;
  public function test3(b:Int):String;
  public function test4(b:Int):Int;
}
