package;

interface BaseInterface {
  public function test():Void;
}

class BaseClass implements BaseInterface {
  public function new() {
    test();
  }

  public function test() {
    trace("Bla");
  }
}

interface IToString extends BaseInterface {
  public function toString():String;
}

class ToStringTestOne extends BaseClass implements IToString {
  public function toString():String {
    return "testOne";
  }
}

class ToStringTestTwo extends BaseClass implements IToString {
  public function toString():String {
    return "testTwo";
  }
}

class TheTest {
  public function new() {
    final toString:IToString = if (true) new ToStringTestOne() else new ToStringTestTwo();
    trace(toString.toString());

    final ternary:IToString = true ? new ToStringTestOne() : new ToStringTestTwo();
    trace(ternary.toString());
  }
}
