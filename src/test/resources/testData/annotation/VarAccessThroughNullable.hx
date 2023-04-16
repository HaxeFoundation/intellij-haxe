package;

class <info descr="">Test</info> {
  var <info descr="">t2</info> : <info descr="">Null</info><<info descr="">Test2</info>>;
  public function <info descr="">new</info>() {
    <info descr="">t2</info> = <info descr="">new</info> <info descr="">Test2</info>();
    trace(<info descr=""><info descr="">t2</info>.s</info>);
    trace(<info descr="">t2</info>.<warning descr="Unresolved symbol">unknown</warning>);
  }
}

class <info descr="">Test2</info> {
  public var <info descr="">s</info> : <info descr="">Null</info><<info descr="">String</info>> = "something";
  public function <info descr="">new</info>() {}
}