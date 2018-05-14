class Test {
  static public var instance = new Test();
  static public var instance2:Test = new Test();
  static public var v:Int = 1;

  public function new() {}

  static public function a(t:Test <error descr="Parameter default type should be constant but was 'Test'">= new Test()</error>) {
}

  static public function b(t = new Test()) {
  }

  static public function c(v = 10) {
  }

  static public function d(v:Int = 10) {
  }
}