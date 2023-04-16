package;
class Test {
  public function new() {
    var <error descr="Incompatible type: Int should be String">shouldWarn:String = 777</error>;
  }
}