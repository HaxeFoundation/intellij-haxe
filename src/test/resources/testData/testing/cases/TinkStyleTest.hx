package cases;

@:asserts
class TinkStyleTest {
  public function new() {}

  public function addsNumbers() {
    asserts.assert(1 + 1 == 2);
    return asserts.done();
  }

  function notPublic() {}

  public static function staticFactory() {}

  @:before
  public function prepare() {}

  @:exclude
  public function skipped() {
    return asserts.done();
  }
}
