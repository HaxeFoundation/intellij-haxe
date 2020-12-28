package;

class Test {
  static inline var myConstant:Float = Std.int(Std.parseFloat("12345".substr(2))) * 1;  // Not really a constant.
  static function doSomething(<error descr="Parameter default type should be constant but was Float">v:Float = myConstant</error>) {}
}