package com.bar;
class Foo {
    public var baz:Baz;
    public function new() {
      baz = new Baz();
    }
}

extern class FooExtern {
  public var fooExtern(default,null):FooExtern;
}
