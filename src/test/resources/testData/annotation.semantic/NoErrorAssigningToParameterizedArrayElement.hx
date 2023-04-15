package;

class TreeNode<T> {

  public function clone() {
    var stack = new Array<TreeNode<T>>();

    stack[0] = this; // Incompatible type: TreeNode<TreeNode> should be TreeNode<T>
  }
}