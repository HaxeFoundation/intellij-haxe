class GetterSetter2 {
  var foo:Int;
  var bar:String;

  @:getter(foo)
  private function get_foo():Int {
    return foo;
  }
  <caret>
}
