package;

class Point {
  var x:Int;
  var y:Int;
  public function new(x:Int,y:Int) {
    this.x=x;
    this.y=y;
  }
}

class Test {
  static function main() {
    var p = new Point(2,1);
    var i:Int;
    i = <error descr="Incompatible type: Point should be Int">p</error>;
  }
}