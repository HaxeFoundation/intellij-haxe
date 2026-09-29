package com.example;

// Conditional compilation makes this file non-stubable, so it is covered by
// the file-based indexes instead of the stub indexes.
class CondClass {
  public var condField:Int;
  public static var condStaticField:String = "x";

  public function new() {}

  public function condMethod():Void {
    #if js
    trace("js");
    #end
  }
}
