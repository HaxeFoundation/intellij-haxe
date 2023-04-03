class ParamInfo4 {
  function main() {
    foo(123123<caret>,123123);
  }

  static function foo(x:Int, y:Int) {
      return "test";
  }
}