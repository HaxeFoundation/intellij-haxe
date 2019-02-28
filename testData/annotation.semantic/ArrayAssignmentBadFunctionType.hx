package;
class Test {
  <error descr="Incompatible type: Array<Int->String> should be Array<Int->Int>">var should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ];</error>
  public function new() {
    var <error descr="Incompatible type: Array<Int->String> should be Array<Int->Int>">should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ]</error>;
  }
}