package;

class Test {
  static inline var myConstant = ((Std.int(2.0) + 10 / 3) - (2 * 1));

  function test(<error descr="Incompatible type: Float should be Int">v:Int = myConstant</error>) {}
}