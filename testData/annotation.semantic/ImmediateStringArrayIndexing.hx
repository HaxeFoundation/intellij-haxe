package;
class Test {
  public static function main() {
    var round:Int = 1;
    var str:String = ["LdrBtnIconB", "LdrBtnIconS", "LdrBtnIconG"][round]; // Error was: "Incompatible type: Array<String> should be String"
    trace(str);
  }
}