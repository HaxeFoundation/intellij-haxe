class ForLoopVariable1 {
  public static function main() {
    var items = [1, 2, 3, 4, 5];
    for (item in items) {
      for (index in 0...item) {
        trace(ite<caret>);
      }
    }
  }
}