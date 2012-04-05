class ForDeclaration2 {
  public static function main() {
    var arr = [1,2,3,4,5,6,7];
    for (foo in 0...arr.length) {
      trace(arr[fo<caret>o]);
    }
  }
}