class ReferenceChain {
  function main() {
    var other = new Other();
    var tmp:MyEnum = other.<caret>
  }
}

class Other {
  public function new() {}
}
