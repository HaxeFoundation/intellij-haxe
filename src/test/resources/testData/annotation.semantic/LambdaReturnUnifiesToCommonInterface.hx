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

class TheTest {
  public function new() {
    final fn:Void->IToString = function() {
      if (true) return new ToStringTestOne();
      return new ToStringTestTwo();
    };
    trace(fn().toString());
  }
}
