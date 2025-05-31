class Test {

  public static function main() {
    var testOk: MyClass = {normalVar: "Hello", defaultProperty: "World", isVar:1};

    var <error descr="Incompatible type: missing member(s) normalVar:String">testMissingNormal: MyClass = {defaultProperty: "World", isVar:1}</error>;
    var <error descr="Incompatible type: missing member(s) defaultProperty:String">testMissingProperty: MyClass = {normalVar: "Hello",  isVar:1}</error>;
    var <error descr="Incompatible type: missing member(s) isVar:Int">testMissingIsVar: MyClass = {normalVar: "Hello", defaultProperty: "World"}</error>;

    var testUnknownExtra: MyClass = <error descr="Incompatible type: {...} should be MyClass">{normalVar: "Hello", defaultProperty: "World", isVar:1, extra: 1}</error>;
  }

}

@:structInit
class MyClass {

  public var normalVar: String;
  public var defaultProperty(default,default): String;
  @:isVar public var isVar(get, set):Int;

  function set_isVar(value:Int):Int {
    return this.isVar = value;
  }


  function get_isVar():Int {
    return isVar;
  }


  // static should be ignored
  public static var staticVar: String;

  // not real var should be ignored
  public var nonRealVar(get, never): String;

  function get_nonRealVar():String {
    return "nonRealVar";
  }
}