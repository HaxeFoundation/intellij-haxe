class Test {
  public function test() {
    var aaa:Demo<Unknown>;
    aaa.push('test');
    aaa.push(<error descr="Can't assign Int = 10 to String">10</error>);
    return aaa;
  }
}

class Demo<T> {
  public function new() { }

  public function push(value:T):Int {
  }
}