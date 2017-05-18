// This test is to prove that a local with the same name as a class
// resolves to the local.

class Test {
    static function main() {
        // Bad practice, but still valid Haxe.
        var A = new Top();
        A.foo(<caret>);
    }
}

class Base<T> {
    public function foo(t:T) {}
}

class Mid<T,U> extends Base<U> {
    public function bar(t:T) {}
    public function baz(u:U) {}
}

class Top extends Mid<Tail,Node> {
    public function new() {}
}

class Node {
}

class Tail {
}

class A {
  public function foo(s:String);
}