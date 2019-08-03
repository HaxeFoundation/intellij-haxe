package;

class TreeNode<T> {
  public var prev:TreeNode<T>;
  public var val:T;

  public function new(val:T) {
    this.val = val;
  }
  public function getLastChild():TreeNode<T> {
    return null;
  }
}

class Test {
  static public function main() {
    var root = new TreeNode<Int>(100);
    var node = root.getLastChild();
    node = node.prev;           // <---- Issue was "Incompatible type TreeNode<T> should be TreeNode<Int>"
  }
}