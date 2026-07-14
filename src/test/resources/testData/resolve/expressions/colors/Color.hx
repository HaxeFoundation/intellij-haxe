package colors;

@:using(colors.Color.ColorTools)
enum Color {
  Red;
  Green;
  Blue;
}

private class ColorTools {
  public static function label(value:Color):String {
    return "color";
  }
}
