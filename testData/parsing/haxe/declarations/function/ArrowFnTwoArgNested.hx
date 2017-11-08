class ArrowFunctionSyntaxTest {
  public static function main() {
    var array = new Array<String>();
    array.map((a) -> a.charCodeAt(0)).sort((a,b) -> a - b);
  }
}