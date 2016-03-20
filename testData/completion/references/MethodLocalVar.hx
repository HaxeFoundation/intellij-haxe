class MethodLocalVar {
  function new() {}

  public function testMethod() {
    var methodLocalVar1:String = "hello";
    var methodLocalVar2:String = "hello";
    <caret>
  }

}