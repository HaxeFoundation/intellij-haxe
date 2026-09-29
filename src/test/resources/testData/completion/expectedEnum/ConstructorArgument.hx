class ConstructorArgument {
  function main() {
    new Holder(<caret>);
  }
}

class Holder {
  public function new(mode:MyEnum) {}
}
