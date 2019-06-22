package;
class Test {
  public function new() {
    var <error descr="Incompatible type: Float should be Int">i:Int = (10.0:Float)</error>;
  }
}