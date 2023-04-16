class CastExpression1 {
  function main() {
    cast(null, Bar).b<caret>ar;
  }
}

class Bar {
  function bar() {
  }
}