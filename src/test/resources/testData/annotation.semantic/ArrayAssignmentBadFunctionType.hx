package;
class Test {
  var should_warn1: Array<Int->Int> = <error descr="Incompatible type: Array<Int->String> should be Array<Int->Int>">[ (x:Int)->{'$x';} ]</error>;
  public function new() {
    var should_warn1: Array<Int->Int> = <error descr="Incompatible type: Array<Int->String> should be Array<Int->Int>">[ (x:Int)->{'$x';} ]</error>;
  }
}