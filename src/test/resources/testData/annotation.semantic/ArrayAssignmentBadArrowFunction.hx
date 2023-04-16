package;
class Test {
  <error descr="Incompatible type: Array<Int->Float> should be Array<Int->Int>">var should_warn3: Array<Int->Int> = [ a -> 1.0 ];</error>
  public function new() {
    var <error descr="Incompatible type: Array<Int->Float> should be Array<Int->Int>">should_warn3: Array<Int->Int> = [ a -> 1.0 ]</error>;
  }
}