package com.example;

// Primary class matching filename: FQN = com.example.SimpleClass
class SimpleClass {
  public var field:Int;
  public static var staticField:String = "hello";
  private var _private:Bool;

  public function new(value:Int) {
    this.field = value;
  }

  public function method():Void {}
  public static function staticMethod():Int { return 0; }
  private function privateMethod():String { return ""; }
  override public function toString():String { return ""; }
}
