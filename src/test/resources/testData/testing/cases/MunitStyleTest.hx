package cases;

class MunitStyleTest {
  public function new() {}

  @Test
  public function addsNumbers():Void {}

  @AsyncTest
  public function loadsAsync():Void {}

  @Ignore("wip")
  @Test
  public function skipped():Void {}

  @Test
  function notPublic():Void {}

  @Test
  public static function staticFactory():Void {}

  public function plainHelper():Void {}
}
