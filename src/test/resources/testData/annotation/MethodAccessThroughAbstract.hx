package;

@:forward
abstract <info descr="null">MyStr</info>(<info descr="null">String</info>) <info descr="null">from</info> <info descr="null">String</info> <info descr="null">to</info> <info descr="null">String</info> {
  public function <info descr="null">doubleLength</info>() { return 2 * this.<info descr="null">length</info>; }
}

class <info descr="null">Test</info> {
  public function <info descr="null">new</info>() {
    var <info descr="null">ms</info> : <info descr="null">MyStr</info> = "something";
    trace(<info descr="null">ms</info>.<info descr="null">toLowerCase</info>);
    trace(<info descr="null">ms</info>.<info descr="null">length</info>);
    trace(<info descr="null">ms</info>.<info descr="null">doubleLength</info>());
    trace(<info descr="null">ms</info>.<warning descr="Unresolved symbol">unknown</warning>());
  }
}