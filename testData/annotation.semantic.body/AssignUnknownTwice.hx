class Test {
  public function test() {
    var a;
    a = 10;
    <error descr="Can't assign String = test to Int">a = 'test'</error>;
    a = 20;
  }
}