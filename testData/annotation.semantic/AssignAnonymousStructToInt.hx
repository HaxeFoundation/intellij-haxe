package;
class Test {
  public function new() {
    var s = {x:1, y:2};
    var i:Int;

    i = <error descr="Incompatible type: Object should be Int"><caret>(s)</error>;
  }
}