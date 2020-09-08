package;

class Test {
  extern inline var myVal(get, null) = 3.14;

  extern public inline function doSomething() {

    inline function internal() {}
    internal();
  }

  inline extern private function get_myVal() {
    return myVal;
  }

  public static function main() {
    var a = new Test();
    a.doSomething();
    a.myVal;
  }

  public function new() {}
}