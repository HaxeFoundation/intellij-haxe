class GetterSetter2 {
  public static var sfoo:Int;
  public var foo:Int;
  var bar:String;

  @:getter(sfoo)
  public static function get_sfoo():Int {
    return sfoo;
  }

  @:getter(foo)
  public function get_foo():Int {
    return foo;
  }
  <caret>
}
