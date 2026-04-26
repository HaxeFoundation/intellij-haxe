package com.example;

import com.example.SimpleClass;
import com.example.IBase;
import com.example.IExtended;

// ChildClass extends SimpleClass and implements IBase, IExtended
// FQN = com.example.ChildClass
class ChildClass extends SimpleClass implements IBase implements IExtended {
  public function new() { super(0); }
  public function baseMethod():Void {}
  public function extendedMethod():String { return ""; }
}
