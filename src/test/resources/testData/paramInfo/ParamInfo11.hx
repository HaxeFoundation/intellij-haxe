class ParamInfo11 {
  function main() {
    testRestArgs("s0",<caret> "s1", "s2", "s3");
  }

  public function testRestArgs(a:String, ...rest:String) {}
}