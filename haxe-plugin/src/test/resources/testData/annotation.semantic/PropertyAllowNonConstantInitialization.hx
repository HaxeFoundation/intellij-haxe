class Test {
  static public var instance = new Test();
  static public var instance2:Test = new Test();
  static public var v:Int = 1;

  static public function a(<error descr="Parameter default type should be constant but was Test">t:Test = new Test()</error>) {
}

  static public function b(t = new Test()) {
  }

  static public function c(v = 10) {
  }

  static public function d(v:Int = 10) {
  }
}