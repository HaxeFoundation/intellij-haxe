class Test {
  public function test() {
    var aaa = new Demo();
    aaa.push('test');
    aaa.push(<error descr="Type mismatch (Expected: 'String' got: 'Int')">10</error>);
    return aaa;
  }
}

class Demo<T> {
  public function new() { }

  public function push(value:T):Int {
  <error descr="Missing return statement">}</error>
}