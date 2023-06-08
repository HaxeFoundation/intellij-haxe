package;

@:forward
abstract <text_attr descr="null">MyStr</text_attr>(<text_attr descr="null">String</text_attr>) from <text_attr descr="null">String</text_attr> to <text_attr descr="null">String</text_attr> {
  public function <text_attr descr="null">doubleLength</text_attr>() { return 2 * this.<text_attr descr="null">length</text_attr>(); }
}

class <text_attr descr="null">Test</text_attr> {
  public function <text_attr descr="null">new</text_attr>() {
    var <text_attr descr="null">ms</text_attr> : <text_attr descr="null">Null</text_attr><<text_attr descr="null">MyStr</text_attr>> = "something";
    trace(<text_attr descr="null">ms</text_attr>.<text_attr descr="null">length</text_attr>());
    trace(<text_attr descr="null">ms</text_attr>.<text_attr descr="null">doubleLength</text_attr>());
    trace(<text_attr descr="null">ms</text_attr>.<warning descr="Unresolved symbol">unknown</warning>());
  }
}