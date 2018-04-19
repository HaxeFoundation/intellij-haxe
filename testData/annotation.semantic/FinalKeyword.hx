package;

class FinalKeyword extends Parent {
  <error descr="Property can not be final">public final v(default, set):Int;</error>
  final w:Int = 10;
  <error descr="Final field 'x' must be initialized immediately or in the constructor">final x:String;</error>
  final y:String;
  final z:Bool;

  public static final CONST_X:String = "";
  <error descr="Static final variable 'CONST_Y' must be initialized">static final CONST_Y:String;</error>

  public function new() {
    y = "Hop hey";
    this.z = true;
  }

  override public function <error descr="Can't override static, inline or final methods">cantBeOverwritten</error>() {}

  function set_v(value:Int):Int {
    return value;
  }
}

class Parent {
  public final function cantBeOverwritten() {}
}