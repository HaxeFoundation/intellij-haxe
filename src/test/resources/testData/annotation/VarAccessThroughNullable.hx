package;

class <info descr="null">Test</info> {
  var <info descr="null">t2</info> : <info descr="null">Null</info><<info descr="null">Test2</info>>;
  public function <info descr="null">new</info>() {
    <info descr="null">t2</info> = <info descr="null">new</info> <info descr="null">Test2</info>();
    trace(<info descr="null">t2</info>.<info descr="null">s</info>);
    trace(<info descr="null">t2</info>.<warning descr="Unresolved symbol">unknown</warning>);
  }
}

class <info descr="null">Test2</info> {
  public var <info descr="null">s</info> : <info descr="null">Null</info><<info descr="null">String</info>> = "something";
  public function <info descr="null">new</info>() {}
}