package;
class Test {
  public function new() {
    var <error descr="Incompatible type: Object should be Int">shouldWarn:Int = {x:2, y:2}</error>;
  }
}