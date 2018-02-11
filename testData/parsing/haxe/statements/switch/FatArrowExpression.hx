package;
class FatArrowExpression {
  public function test() {
    var a = map1[0];
    var b:Int = 0;

    switch a
    {
      // This should parse as a fatArrowExpression.
      case b => _: trace(b);
    }
  }
}