package;
class Test {
  public function new() {
    var f:Float;
    f = (10:Int);
    var i:Int;
    i = <error descr="Incompatible type: Float should be Int">(10:Float)</error>;
  }
}
