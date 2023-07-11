package;

class Test {
// TODO mlo:  should result in : Inline variable initialization must be a constant value
  static inline var myConstant:Float = Std.int(Std.parseFloat("12345".substr(2))) * 1;  // Not really a constant.
  static function doSomething(v:Float = myConstant) {}
}