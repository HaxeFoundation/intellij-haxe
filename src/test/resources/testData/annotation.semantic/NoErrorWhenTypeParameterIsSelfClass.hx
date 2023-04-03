package;

interface Comparable<T> {
  function compare(that:T):Int;
}

class Node implements Comparable<Node> {
  public var myVal:Int;
  public function new(id:Int) {
    this.id = id;
  }
  public function compare(that:Node):Int {  // Original issue was error around "that:Node"
    return id - that.id;
  }
}