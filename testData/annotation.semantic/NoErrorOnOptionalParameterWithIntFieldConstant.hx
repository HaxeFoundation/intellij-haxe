package;

class Test {
  static inline var myConstant:Int = 4;

  public function someFunction(param:Int = myConstant):Void {
     // The "param:Int = myConstant" gave an Error "Parameter default type should be constant but was Int"
  }
}