package com.example.module;

// Module-level class: FQN = com.example.module.ModuleClass
class ModuleClass {
  public var classField:String;
  public function new() {}
  public function classMethod():Void {}
}

// Module-level function (outside any class)
function moduleFunction(x:Int):Int {
  return x * 2;
}

// Module-level variable
var moduleVar:String = "module";
