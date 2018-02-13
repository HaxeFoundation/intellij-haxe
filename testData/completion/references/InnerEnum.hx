@:enum
abstract AbstractEnum(String) {
  var ONE = "one";
  var TWO = "two";
  var THREE = "three";
}

enum OrdinaryEnum {
  ONE;
  TWO;
  THREE;
}

class InnerEnum {
  public function new() {
    <caret>
  }
}