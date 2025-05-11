package;
class Test {
  public function new() {
    var f = 1.0;
    var i:Int = <error descr="Incompatible type: Float should be Int">(f)</error>;
  }
}