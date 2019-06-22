class Test {
  public function test() {
    var a;
    a = 10;
    a = <error descr="Incompatible type: String should be Int">'test'</error>;
    a = 20;
  }
}