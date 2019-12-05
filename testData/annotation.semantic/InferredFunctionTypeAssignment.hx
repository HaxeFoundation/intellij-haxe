package;

class Test {
  static function main() {

    var callback3 = ()->1;
    callback3 = ()->2;
    callback3 = <error descr="Incompatible type: Void->String should be Void->Int">()->""</error>; // Should be an error, because the type was inferred as Void->Int.

  }
}
