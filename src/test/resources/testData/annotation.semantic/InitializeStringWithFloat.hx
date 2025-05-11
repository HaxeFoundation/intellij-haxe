package;
class Test {
  public function new() {
    var shouldWarn:String = <error descr="Incompatible type: Float should be String">3.14159</error>;
  }
}