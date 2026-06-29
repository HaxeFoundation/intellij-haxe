class ParamInfo11 {
  function main() {
    testRestArgs("s0", "s1", <caret>"s2", "s3");
  }

  public function testRestArgs(a:String, ...rest:String) {}
}