package;
class Test {
  <error descr="Incompatible type Array<Int -> Int> can't be assigned from Array<Int -> Float = 1.0>">var should_warn3: Array<Int->Int> = [ a -> 1.0 ];</error>
  public function new() {
    var <error descr="Incompatible type Array<Int -> Int> can't be assigned from Array<Int -> Float = 1.0>">should_warn3: Array<Int->Int> = [ a -> 1.0 ]</error>;
  }
}