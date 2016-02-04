class Methods {
  public var test1:Int;
  public var test2(default, null):String;
  public var test3(get, never):Float;
  public var test4:Int = 7;
  public var test5 = 0xFFFFFFFF;
  public var test6 = 1e300;
}

class Main {
  public static function main() {
    new Methods().<caret>
  }
}