class ArrowFunctionSyntaxTest {
  public static function main() {
    var array = new Array<String>();
    array.map(function(a) a.charCodeAt(0));
    array.map(a -> a.charCodeAt(0));
  }
}