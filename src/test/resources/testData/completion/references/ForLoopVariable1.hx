class ForLoopVariable1 {
  public static function main() {
    var items = [1, 2, 3, 4, 5];
    for (item in items) {
      trace(it<caret>);
    }
  }
}