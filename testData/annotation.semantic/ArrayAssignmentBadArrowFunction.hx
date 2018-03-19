package;
class Test {
  <error descr="Incompatible type Array<(Int) -> Int> can't be assigned from Array<(Int) -> Float>">var should_warn3: Array<Int->Int> = [ a -> 1.0 ];</error>
  public function new() {
    var <error descr="Incompatible type Array<(Int) -> Int> can't be assigned from Array<(Int) -> Float>">should_warn3: Array<Int->Int> = [ a -> 1.0 ]</error>;
  }
}