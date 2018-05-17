class Test {
  public var a(<error descr="Can't find method get_a">get</error>, <error descr="Can't find method set_a">set</error>):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
  @:isVar public var b(<error descr="Can't find method get_b">get</error>, <error descr="Can't find method set_b">set</error>):Int = 10;
  public var c(<error descr="Can't find method get_c">get</error>, default):Int = 10;
  public var d(<error descr="Can't find method get_d">get</error>, null):Int = 10;
  public var e(<error descr="Can't find method get_e">get</error>, never):Int <error descr="This field cannot be initialized because it is not a real variable">= 10</error>;
}

class Test2 {
  public var a(<error descr="Invalid getter accessor">set</error>, never):Int;
  public var b(never, <error descr="Invalid setter accessor">get</error>):Int;
}

interface ITest {
  var a(get, set):Int;
}