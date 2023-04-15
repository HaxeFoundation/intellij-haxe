class Test {
  static function main() {
    new ConstructorGenericParameters<Node>(10,<caret> new Node());
  }
}

class ConstructorGenericParameters<T> {
  public function new(a:Int, b:Bool = false, ?c:Float, ?d:T) {

  }
}

class Node {
}