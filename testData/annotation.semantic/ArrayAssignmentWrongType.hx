package;
class Test {
  <error descr="Incompatible type Array<Int->Int> can't be assigned from Array<String>">var should_warn2: Array<Int->Int> = [ "one", "two"];</error>
  public function new() {
    var <error descr="Incompatible type Array<Int->Int> can't be assigned from Array<String>">should_warn2: Array<Int->Int> = [ "one", "two"]</error>;
  }
}