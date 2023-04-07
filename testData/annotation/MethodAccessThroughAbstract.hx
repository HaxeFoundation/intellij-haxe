package;

@:forward
abstract <info descr="">MyStr</info>(<info descr="">String</info>) from <info descr="">String</info> to <info descr="">String</info> {
  public function <info descr="">doubleLength</info>() { return 2 * <info descr="">this.length</info>(); }
}

class <info descr="">Test</info> {
  public function <info descr="">new</info>() {
    var <info descr="">ms</info> : <info descr="">MyStr</info> = "something";
    trace(<info descr=""><info descr="">ms</info>.length</info>());
    trace(<info descr=""><info descr="">ms</info>.doubleLength</info>());
    trace(<info descr="">ms</info>.<warning descr="Unresolved symbol">unknown</warning>());
  }
}