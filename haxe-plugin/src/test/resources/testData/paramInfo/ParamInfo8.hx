class Test {
    static function main() {
        var a = new Top();
        a.foo(<caret>);
    }
}

class Base<T> {
    public function foo(t:T) {}
}

class Top extends Base<Node> {
    public function new () {}
}

class Node {
}