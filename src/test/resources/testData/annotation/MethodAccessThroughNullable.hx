package;

class <info descr="null">Test</info> {
  var <info descr="null">ns</info> : <info descr="null">Null</info><<info descr="null">String</info>> = "Something";
  public function <info descr="null">new</info>() {
    trace(<info descr="null">ns</info>.<info descr="null">length</info>);
    trace(<error descr="Int is not a callable type"><info descr="null">ns</info>.<info descr="null">length</info>()</error>);
    trace(<info descr="null">ns</info>.<warning descr="Unresolved symbol">unknown</warning>());
  }
}