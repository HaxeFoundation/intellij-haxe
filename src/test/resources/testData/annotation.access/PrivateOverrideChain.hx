package;

class Base {
  function run() {}
}

class Middle extends Base {
  override function run() {}
}

class Leaf extends Middle {
  override function run() {}
}

class Outsider {
  function call(leaf:Leaf) {
    leaf.<error descr="Cannot access private field run">run</error>();
  }
}
