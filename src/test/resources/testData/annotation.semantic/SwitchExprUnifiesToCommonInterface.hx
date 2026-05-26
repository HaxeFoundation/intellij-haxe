package;

interface BaseInterface {
  public function test():Void;
}

class BaseClass implements BaseInterface {
  public function new() {}
  public function test() {}
}

interface IToString extends BaseInterface {
  public function toString():String;
}

class ToStringTestOne extends BaseClass implements IToString {
  public function toString():String { return "one"; }
}

class ToStringTestTwo extends BaseClass implements IToString {
  public function toString():String { return "two"; }
}

class ToStringTestThree extends BaseClass implements IToString {
  public function toString():String { return "three"; }
}

class TheTest {
  public function new() {
    final n = 1;
    final x:IToString = switch (n) {
      case 1: new ToStringTestOne();
      case 2: new ToStringTestTwo();
      case _: new ToStringTestThree();
    };
    trace(x.toString());
  }
}
