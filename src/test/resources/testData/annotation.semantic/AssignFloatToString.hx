package;
class Test {
  public function new() {
    var shouldWarn:String;
    shouldWarn = <error descr="Incompatible type: Float should be String">10/2</error>;
  }
}