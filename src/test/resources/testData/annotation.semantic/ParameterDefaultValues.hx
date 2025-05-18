package;

class ConstParams {

  //Correct
  function testOK1(v = 1) { return v;}
  function testOk2(v = "1") {return v;}
  function testOk3(v = EnumVal) {return v;}
  function testOk4(v = (2 + 3)) {return v;}
  function testOk5(v = inlineVar) {return v;}
  function testOk6(v = (2 + inlineVar)) {return v;}
  function testOk7(v = enumValue ) {return v;}

  //WRONG
  function testWrong1(v:Array<String> = <error descr="Default argument value should be constant">["1"]</error>) {return v;} // Default argument value should be constant
  function testWrong2(v = <error descr="Default argument value should be constant">{a:"1" }</error>) {return v;} //Default argument value should be constant
  function testWrong3(v = <error descr="Default argument value should be constant">normalVar</error>) {return v;} //Default argument value should be constant
  function testWrong4(v = <error descr="Default argument value should be constant">finalVar</error>) {return v;} // Default argument value should be constant
  function testWrong5(v = <error descr="Default argument value should be constant">EnumConstructor( 1)</error>) {return v;} // Default argument value should be constant
  function testWrong6(v = (2 + <error descr="Default argument value should be constant">normalVar</error>)) {return v;}
  function testWrong7(v = <error descr="Default argument value should be constant">enumValue.getter</error>) {return v;}



  var normalVar = getValue();
  static function getValue():Int{return 1;}
  inline static var inlineVar = 10;
  static final finalVar = 10;
}

enum TestNum {EnumVal; EnumConstructor(x:Int);}
@:enum abstract  TestAbsEnum {
  var enumValue = 1;

  static public var staticGetter(get, never):String;
  static function get_staticGetter() return "string";

  public var getter(get, never):String;
  function get_getter() return "string";

}
