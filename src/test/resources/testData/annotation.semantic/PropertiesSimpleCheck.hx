import sys.thread.Thread.ThreadImpl;
class Test {
  public var a(<error descr="Can't find getter method">get</error>, <error descr="Can't find setter method">set</error>):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
  @:isVar public var b(<error descr="Can't find getter method">get</error>, <error descr="Can't find setter method">set</error>):Int = 10;
  public var c(<error descr="Can't find getter method">get</error>, default):Int = 10;
  public var d(<error descr="Can't find getter method">get</error>, null):Int = 10;
  public var e(<error descr="Can't find getter method">get</error>, never):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
}

class Test2 {
  public var a(<error descr="Invalid getter accessor">set</error>, never):Int;
  public var b(never, <error descr="Invalid setter accessor">get</error>):Int;
}

interface ITest {
  var a(get, set):Int;
}


abstract ATest(Dynamic) {
  // normal abstracts should implement getter/setters
  public var value(<error descr="Can't find getter method">get</error>,never):Dynamic;
}

extern abstract ATest(Dynamic) {
// extern abstracts do not require implementations
  public var value(get,never):Dynamic;
}