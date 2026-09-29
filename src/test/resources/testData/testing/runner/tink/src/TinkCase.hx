@:asserts
class TinkCase {
  public function new() {}

  public function passes() {
    asserts.assert(1 + 1 == 2);
    return asserts.done();
  }

  public function fails() {
    asserts.assert(1 + 1 == 3);
    return asserts.done();
  }
}
