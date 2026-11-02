package;

class First extends Second {
  override function run() {}
}

class Second extends First {
  override function run() {}
}

class Outsider {
  function call(first:First) {
    first.<error descr="Cannot access private field run">run</error>();
  }
}
