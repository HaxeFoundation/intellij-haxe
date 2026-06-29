class ParamInfo11 {
  function main() {
    testRestArgs("s0", "s1", "s2", "s3"<caret>);
  }

  public function testRestArgs(a:String, ...rest:String) {}
}