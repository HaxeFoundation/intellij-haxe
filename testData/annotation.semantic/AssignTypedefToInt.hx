package;

typedef Pt = {var x:Int; var y:Int;};

class Test {
  static function main() {
    var p : Pt = {x:1, y:1};
    var i : Int;
    i = <error descr="Incompatible type: Pt should be Int">p</error>;
  }
}