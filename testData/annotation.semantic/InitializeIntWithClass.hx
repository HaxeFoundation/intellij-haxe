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
    var <error descr="Incompatible type: Point should be Int">i : Int = new Point(2,1)</error>;
  }
}