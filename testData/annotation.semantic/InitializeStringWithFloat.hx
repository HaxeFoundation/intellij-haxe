package;
class Test {
  public function new() {
    var <error descr="Incompatible type: Float should be String">shouldWarn:String = 3.14159</error>;
  }
}