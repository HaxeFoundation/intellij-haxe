package unit;

// Project-local base (the crypto shape): detection must resolve the chain
// unit.Test -> utest.Test -> utest.ITest, not just a direct extends.
class Test extends utest.Test {
  public function new() {
    super();
  }
}
