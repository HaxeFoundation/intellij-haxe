package;
class Test {
  public function new() {
    var shouldWarn:Float;
    shouldWarn = <error descr="Incompatible type: String should be Float">"3.14159"</error>;
  }
}