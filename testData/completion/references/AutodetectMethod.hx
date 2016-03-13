class Methods {
  public var mybool1 = false;
  public var mybool2:Bool = false;
  public function foo() return 10;
  public function foo2() {
    return 10.0;
  }
  public function foo3() {
    return this.foo() == 10;
  }
}

class Main {
  public static function main() {
    new Methods().<caret>
  }
}