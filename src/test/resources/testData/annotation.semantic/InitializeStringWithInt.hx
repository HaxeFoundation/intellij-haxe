package;
class Test {
  public function new() {
    var shouldWarn:String = <error descr="Incompatible type: Int should be String">777</error>;
  }
}