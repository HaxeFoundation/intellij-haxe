class Test {
  public var a(<error descr="Accessor method 'get_a' not found">get</error>, <error descr="Accessor method 'set_a' not found">set</error>):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
  @:isVar public var b(<error descr="Accessor method 'get_b' not found">get</error>, <error descr="Accessor method 'set_b' not found">set</error>):Int = 10;
  public var c(<error descr="Accessor method 'get_c' not found">get</error>, default):Int = 10;
  public var d(<error descr="Accessor method 'get_d' not found">get</error>, null):Int = 10;
  public var e(<error descr="Accessor method 'get_e' not found">get</error>, never):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
}

class Test2 {
  public var a(<error descr="Invalid getter accessor type. Only 'get', 'default', 'null', 'never', 'dynamic' types allowed">set</error>, never):Int;
  public var b(never, <error descr="Invalid setter accessor type. Only 'set', 'default', 'null', 'never', 'dynamic' types allowed">get</error>):Int;
}

interface ITest {
  var a(get, set):Int;
}