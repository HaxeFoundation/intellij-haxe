package;

class Base< P > {
  public static function doSomething(val:P) : P {
    var copy : P = val;
  }
}

class Main {
  public static function main() {
    var m = Base<Int>.doSomething();
  }
}