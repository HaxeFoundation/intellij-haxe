package;

class Test {
  static inline var myConstant = "123" + (Std.string("456") + "789");

  public function someFunction(param:String = myConstant):Void {
    // The "param:String = myConstant" gave an Error "Parameter default type should be constant but was String"
  }
}
