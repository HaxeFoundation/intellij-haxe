typedef Null<T> = T;
class Bar<T> {
  public var s:T;
}

class Main {
  function foo() {
    var a:Null<String>;
    a.<caret>
  }
}