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
  public static function pick<T>(a:T, b:T):T {
    return a;
  }

  public function new() {
    final picked:IToString = pick(new ToStringTestOne(), new ToStringTestTwo());
    trace(picked.toString());

    final pickedBase:BaseClass = pick(new ToStringTestOne(), new ToStringTestTwo());
    trace(pickedBase);

    final viaParent:BaseClass = pick(new ToStringTestOne(), new BaseClass());
    trace(viaParent);

    final viaParentReversed:BaseClass = pick(new BaseClass(), new ToStringTestOne());
    trace(viaParentReversed);
  }
}
