package com.example;

// Abstract type: FQN = com.example.MyAbstract
abstract MyAbstract(Int) from Int to Int {
  public function new(v:Int) this = v;
  public function getValue():Int { return this; }
}
