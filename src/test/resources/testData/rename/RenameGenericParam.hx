package;

class Base< <caret>T > {
  public static function doSomething(val:T) : T {
    var copy : T = val;
  }
}

class Main {
  public static function main() {
    var m = Base<Int>.doSomething();
  }
}