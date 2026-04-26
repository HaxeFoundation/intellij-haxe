package com.example;

// Private class (alongside primary): primary class is PrivateClassModule, private is PrivateClass
// PrivateClassModule FQN = com.example.PrivateClassModule
// PrivateClass FQN = com.example.PrivateClassModule.PrivateClass (ancillary)
class PrivateClassModule {
  public function new() {}
}

private class PrivateClass {
  public var value:Int;
  public function new() { value = 0; }
}
