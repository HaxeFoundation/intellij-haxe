class ForLoopVariable3 {
  public static function main() {
    var items = [1, 2, 3, 4, 5];
    var inept:String = "blah";
    for (item in items) {
      for (index in 0...item) {
        var inverse:Bool = true;
        trace(in<caret>);
      }
    }
  }
}