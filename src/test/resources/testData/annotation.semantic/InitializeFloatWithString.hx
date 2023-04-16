package;
class Test {
  public function new() {
    var <error descr="Incompatible type: String should be Float">shouldWarn:Float = "3.14159"</error>;
  }
}