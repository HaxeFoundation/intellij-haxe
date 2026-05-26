package;

class TestClass {
  public function new() {}
}

class Injector {
  public function new() {}

  // Bodyless macro stub. The actual implementation lives in a sibling macro
  // class and uses `Context.getExpectedType()` to determine the return type
  // per call site. The IntelliJ plugin must not treat the missing body as a
  // signal that this method returns Void.
  public macro function provide(_);
}

class TheTest {
  public function new() {
    var injector = new Injector();
    final test:TestClass = injector.provide();
    trace(test);
  }
}
