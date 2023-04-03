package;

class LinkedQueueNode<T> {
  public function new() {}
}

class LinkedQueue<T> {
  var mHead:LinkedQueueNode<T>;
  var mTail:LinkedQueueNode<T>;
}

class Test {
  public function main() {
    var l = new LinkedQueue<Int>();
    var h:LinkedQueueNode<Int> = l.mHead;
    var t:LinkedQueueNode<Int> = l.mTail;
  }
}