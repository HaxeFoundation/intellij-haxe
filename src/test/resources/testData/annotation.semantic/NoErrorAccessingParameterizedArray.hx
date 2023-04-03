// Original source for this code was found in the polygonal library.
package;

class TreeNode<T> {
  public var val:T;
  public var next:TreeNode<T>;
  public var children:TreeNode<T>;

  public function new(val:T) {
    this.val = val;
  }

  // There were several errors throughout this method in a transitional stage of the code.
  // Thus, this makes a good test that we didn't break things.  :)
  //
  public function iter(f:T->Void, ?tmpStack:Array<TreeNode<T>>):TreeNode<T>
  {
    var stack = tmpStack;
    if (stack == null) stack = [];
    stack[0] = this;
    var top = 1;
    while (top > 0)
    {
      var n = stack[--top];
      var c = n.children;
      while (c != null)
      {
        stack[top++] = c;
        c = c.next;
      }
      f(n.val);
    }
    return this;
  }
}

class Test {
  public static function main() {
    var tree = new TreeNode<Int>(1);

    tree.iter((val)->{trace(val);}, [ tree ]);
  }
}
