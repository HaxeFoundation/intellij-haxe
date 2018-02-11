package;
class MapLiteral {
  var words = ["one", "two", "three"];
  var map = [
    for (n in 0...words.length)
      n => words[n]
    ];
}