class ParamInfo3 {
  function main() {
    foo(12<caret>3123,123123);
  }

  static function foo(x:Int, y:Int) {
      return "test";
  }
}