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
    final x:IToString = try {
      new ToStringTestOne();
    } catch (e:Dynamic) {
      new ToStringTestTwo();
    };
    trace(x.toString());
  }
}
