class AtConstTest {
  public static function main() {
    new Test<String>(); // Invalid, but should parse OK.
  }
}

class Test<@:const T> {
  public static function main() {
    var t =  new Test<124>();  // <---- Original errors on '<' and ';'
  }
  public function new() {}
}
