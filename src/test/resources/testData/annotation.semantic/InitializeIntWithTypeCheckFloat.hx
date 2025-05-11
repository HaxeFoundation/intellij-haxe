package;
class Test {
  public function new() {
    var i:Int = <error descr="Incompatible type: Float should be Int">(10.0:Float)</error>;
  }
}