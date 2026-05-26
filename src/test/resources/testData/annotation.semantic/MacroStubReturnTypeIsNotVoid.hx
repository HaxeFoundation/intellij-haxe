package;

class TestClass {
  public function new() {}
}

class Factory {
  public function new() {}

  // Bodyless macro stub. The actual implementation lives in a sibling macro
  // class and uses `Context.getExpectedType()` to determine the return type
  // per call site. The IntelliJ plugin must not treat the missing body as a
  // signal that this method returns Void.
  public macro function make(_);
}

class TheTest {
  public function new() {
    var factory = new Factory();
    final test:TestClass = factory.make();
    trace(test);
  }
}
