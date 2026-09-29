package cases;

import massive.munit.Assert;

class MunitCase {
  public function new() {}

  @Test
  public function testPasses():Void {
    trace("hello from the passing test");
    Assert.isTrue(true);
  }

  @Test
  public function testFails():Void {
    Assert.areEqual(1, 2);
  }

  @Ignore("not ready")
  @Test
  public function testIgnored():Void {}
}
