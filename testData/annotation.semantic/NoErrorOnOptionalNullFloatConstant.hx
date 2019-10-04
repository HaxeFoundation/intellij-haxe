// This tests several things:
//   - that the Null<Float> constant is recognized as constant.
//   - that the "Float" parameter is not overridden by the class parameter.
//   - that a negative number is allowed as a constant. (It is parsed as two tokens.)

class Test<T:String> {   // Constraint here forces a type on T.

  public static inline var f:Null<Float> = -20;
  public function fFunction(i:Float = f) {}

}