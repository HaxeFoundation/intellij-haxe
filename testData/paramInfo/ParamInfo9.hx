class Test {
  static function main() {
    new ConstructorParameters(10, <caret>0.0);
  }
}

class ConstructorParameters {
  public function new(a:Int, b:Bool = false, ?c:Float, ?d) {

  }
}