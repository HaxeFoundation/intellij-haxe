package;

class <text_attr descr="null">Test</text_attr> {
  var <text_attr descr="null">t2</text_attr> : <text_attr descr="null">Null</text_attr><<text_attr descr="null">Test2</text_attr>>;
  public function <text_attr descr="null">new</text_attr>() {
    <text_attr descr="null">t2</text_attr> = <text_attr descr="null">new</text_attr> <text_attr descr="null">Test2</text_attr>();
    trace(<text_attr descr="null">t2</text_attr>.<text_attr descr="null">s</text_attr>);
    trace(<text_attr descr="null">t2</text_attr>.<warning descr="Unresolved symbol">unknown</warning>);
  }
}

class <text_attr descr="null">Test2</text_attr> {
  public var <text_attr descr="null">s</text_attr> : <text_attr descr="null">Null</text_attr><<text_attr descr="null">String</text_attr>> = "something";
  public function <text_attr descr="null">new</text_attr>() {}
}