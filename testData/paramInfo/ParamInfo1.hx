class ParamInfo1 extends Inner<Node> {
  function foo() {
    bar(<caret>);
  }
}

class Inner<T> {
  public function bar(p1:Int, p2, p3:T) {

  }
}

class Node {

}