class Test {
  static function main() {
    var s : Int = 2;
    s >>= 2;  // <---- '>>=' wasn't parsed correctly.
  }
}