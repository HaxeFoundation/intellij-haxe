class StaticsInExtended extends BaseClass {

  public static var MY_STATIC_FIELD:String = "test";

  @:isVar public static var myStaticProperty(get, set):String;

  static function get_myStaticProperty():String {
    return myStaticProperty;
  }

  static function set_myStaticProperty(value:String) {
    myStaticProperty = value;
  }

  public function new() { super(); }

  public static function myStaticMethod() {}

}

class BaseClass {

  public static var MY_STATIC_FIELD:String = "test";

  @:isVar public static var myStaticProperty(get, set):String;

  static function get_myStaticProperty():String {
    return myStaticProperty;
  }

  static function set_myStaticProperty(value:String) {
    myStaticProperty = value;
  }

  public function new() {}

  public static function myStaticMethod() {}

}