package;
class MapLiteralComprehensionWhile {
  public function MapLiteral() {
    var words = ["one", "two", "three"];
    var i = 0;
    var map = [
      while (i < words.length)
        i => words[i++]
    ];
  }
}