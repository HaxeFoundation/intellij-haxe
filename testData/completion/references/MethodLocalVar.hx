class MethodLocalVar {
  function new() {}

  public function testMethod() {
    var localVar1:String = "hello";
    var localVar2 = "hello";
    <caret>
  }

}