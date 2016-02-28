package test;

@:enum abstract SampleAbstractEnum(Int) from Int to Int {
  var ONE = 1;
  public var TWO = 2;
  var THREE:Int = 3;
  public var FOUR:SampleAbstractEnum = 4;

  static public var staticGetter(get, never):String;
  static function get_staticGetter() return "5";

  public var getter(get, never):String;
  function get_getter() return "6";
}