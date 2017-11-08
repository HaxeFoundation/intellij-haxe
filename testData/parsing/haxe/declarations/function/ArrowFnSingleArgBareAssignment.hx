class ArrowFunctionSyntaxTest {
  public static function main() {
    var normal = function(a) a.toInt();
    var arrow = a -> a.toInt();
  }
}