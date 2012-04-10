class GetterSetter2 {
  var foo:Int, bar:String;

  @:getter(foo)
  public function getFoo():Int {
    return foo;
  }
  <caret>
}