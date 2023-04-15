package;
class Test {
  public function new() {
    var shouldWarn:Int;
    shouldWarn = <error descr="Incompatible type: Float should be Int">10/2</error>;
  }
}