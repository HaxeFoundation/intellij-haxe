package cases;

import utest.ITest;

// utest's interface style: no base class, just the marker interface.
class InterfaceStyleTest implements ITest {
  public function new() {}

  public function testDirect():Void {}
}
