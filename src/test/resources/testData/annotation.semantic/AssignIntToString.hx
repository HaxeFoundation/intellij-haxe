package;
class Test {
  public function new() {
    var shouldWarn:String;
    shouldWarn = <error descr="Incompatible type: Int should be String">10</error>;
  }
}