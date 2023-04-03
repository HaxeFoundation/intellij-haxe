package;
class Test {
  public function new() {
    var <error descr="Incompatible type: Float should be Int">shouldWarn:Int = 10/2</error>;
  }
}