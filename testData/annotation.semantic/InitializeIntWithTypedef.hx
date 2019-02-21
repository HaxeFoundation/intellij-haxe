package;

typedef Pt = {var x:Int; var y:Int;};

class Test {
  static function main() {
    var p : Pt = {x:1, y:1};
    var <error descr="Incompatible type: Pt should be Int">i : Int = p</error>;
  }
}