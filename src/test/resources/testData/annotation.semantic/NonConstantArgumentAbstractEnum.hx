import test.SampleAbstractEnum;
class NonConstantArgumentAbstractEnum {
  function test1(arg:SampleAbstractEnum = SampleAbstractEnum.ONE) {}
  function test3(arg:Dynamic = SampleAbstractEnum.THREE) {}
  function test2(arg:Int = SampleAbstractEnum.TWO) {}
  function test4(arg:SampleAbstractEnum = 4) {}
}