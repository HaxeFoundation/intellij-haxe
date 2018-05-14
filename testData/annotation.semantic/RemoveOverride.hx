package ;

class RemoveOverride {
  <error descr="Method overrides nothing">override<caret></error> public function testMethod() {
  }
}