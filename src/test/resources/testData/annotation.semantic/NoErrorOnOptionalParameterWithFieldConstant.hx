package;

class Test {
  static inline var myConstant:Int = 4;

  public function someFunction(param = myConstant):Void {

  }

  public function new() {
    someFunction();
    someFunction(1);
    someFunction(<error descr="Could not find parameter accepting 'String' ">""</error>);// Wrong: incorrect parameter type
  }
}