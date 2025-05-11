package;
class Test {
  var should_warn3: Array<Int->Int> = <error descr="Incompatible type: Array<unknown->Float> should be Array<Int->Int>">[ a -> 1.0 ]</error>;
  public function new() {
    var should_warn3: Array<Int->Int> = <error descr="Incompatible type: Array<unknown->Float> should be Array<Int->Int>">[ a -> 1.0 ]</error>;
  }
}