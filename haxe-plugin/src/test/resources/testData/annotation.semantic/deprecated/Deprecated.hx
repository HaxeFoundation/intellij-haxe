class Deprecated {
  @:deprecated("Use bs instead.")
  public static var variableStaticText:String;

  @:deprecated("Use cs instead.")
  public static var propertyStaticText(get, set):String;

  @:deprecated("Use as instead.")
  public static function methodStaticText():String {
  }

  @:deprecated("Use b instead.")
  public var variableText:String;

  @:deprecated("Use c instead.")
  public var propertyText(get, set):String;

  @:deprecated("Use a instead.")
  public function methodText():String {
  }

  @:deprecated
  public static var variableStatic:String;

  @:deprecated
  public static var propertyStatic(get, set):String;

  @:deprecated
  public static function methodStatic():String {
  }

  @:deprecated
  public var variable:String;

  @:deprecated
  public var property(get, set):String;

  @:deprecated
  public function method():String {
  }
}