class StaticsInExtended extends BaseClass {

  public static var <warning descr="Field 'MY_STATIC_FIELD' overrides a static field of a superclass.">MY_STATIC_FIELD</warning>:String = "test";

  @:isVar public static var <warning descr="Field 'myStaticProperty' overrides a static field of a superclass.">myStaticProperty</warning>(get, set):String;

  static function <warning descr="Method 'get_myStaticProperty' overrides a static method of a superclass">get_myStaticProperty</warning>():String {
    return myStaticProperty;
  }

  static function <warning descr="Method 'set_myStaticProperty' overrides a static method of a superclass">set_myStaticProperty</warning>(value:String) {
    myStaticProperty = value;
  }

  public function new() { super(); }

  public static function <warning descr="Method 'myStaticMethod' overrides a static method of a superclass">myStaticMethod</warning>() {}

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