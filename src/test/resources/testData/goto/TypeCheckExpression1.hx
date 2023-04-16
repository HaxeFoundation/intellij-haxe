class TypeCheckExpression1 {
  function main() {
    (null: Bar).b<caret>ar;
  }
}

class Bar {
  function bar() {
  }
}