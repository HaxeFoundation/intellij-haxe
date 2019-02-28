package;
class Test {
  <error descr="Incompatible type: Array<String> should be Array<Int->Int>">var should_warn2: Array<Int->Int> = [ "one", "two"];</error>
  public function new() {
    var <error descr="Incompatible type: Array<String> should be Array<Int->Int>">should_warn2: Array<Int->Int> = [ "one", "two"]</error>;
  }
}