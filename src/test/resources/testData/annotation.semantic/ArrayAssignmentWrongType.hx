package;
class Test {
  var should_warn2: Array<Int->Int> = <error descr="Incompatible type: Array<String> should be Array<Int->Int>">[ "one", "two"]</error>;
  public function new() {
    var should_warn2: Array<Int->Int> = <error descr="Incompatible type: Array<String> should be Array<Int->Int>">[ "one", "two"]</error>;
  }
}