class Test {
  private static var STATIC_CONST = new A();
  private var field:A = new A();

  public function new() {
    test(new A());
  }

  public function test(value:A) {

  }
}

class A {
  public function new<caret>() {

  }
}