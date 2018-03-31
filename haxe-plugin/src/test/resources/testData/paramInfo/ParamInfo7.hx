class Test {
    static function main() {
        var a = new Top();
        a.foo(<caret>);
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