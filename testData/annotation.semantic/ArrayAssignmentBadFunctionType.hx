package;
class Test {
  <error descr="Incompatible type Array<Int->Int> can't be assigned from Array<Int->String>">var should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ];</error>
  public function new() {
    var <error descr="Incompatible type Array<Int->Int> can't be assigned from Array<Int->String>">should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ]</error>;
  }
}