class ParamInfo6 {
  function main() {
    foo(123123,<caret>);
  }

  static function foo(x:Int, y:Int = 239) {
      return "test";
  }
}