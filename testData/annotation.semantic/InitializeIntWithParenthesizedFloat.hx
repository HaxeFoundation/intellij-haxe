package;
class Test {
  public function new() {
    var f = 1.0;
    var <error descr="Incompatible type: Float should be Int">i:Int =<caret> (f)</error>;
  }
}