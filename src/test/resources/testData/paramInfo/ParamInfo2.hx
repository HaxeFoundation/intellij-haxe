class ParamInfo2 extends Inner<Node> {
  function foo() {
    bar(10, "", <caret>);
  }
}

class Inner<T> {
  public function bar(p1:Int, p2, p3:T) {

  }
}

class Node {

}