package;
enum Test {
  TString(s:String);
  TInt(i:Int);
}
class SwitchExtractorExpression {
    public function test() {
      var e = TString("fOo");
      var success = switch(e) {
        case TString(_.toLowerCase() => "foo"):
          true;
        case _:
          false;
      }
    }
}