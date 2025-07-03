package ;

class TestSuperNotNessesary extends  BaseClassWithoutConstructor {
  public function new() {} // CORRECT
}
class TestSuperNessesary extends  BaseClassWithConstructor {
  public function new() {super();} // CORRECT
}



class TestMissingSuper extends  BaseClassWithConstructor {
  public function <error descr="Missing super constructor call">new</error>() {} // WRONG super reqiured
}
class TestUnexpectedSuper {
  public function new() {<error descr="Current class does not have a super">super()</error>;} // WRONG unexpected super
}


class BaseClassWithoutConstructor { }
class BaseClassWithConstructor {
  public function new() {}
}