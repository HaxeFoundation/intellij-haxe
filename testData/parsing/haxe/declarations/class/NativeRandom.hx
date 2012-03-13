@:final extern class NativeRandom {
  public static function rand():Int;

  @:macro @:require(something) public static function rand(n:Int):Int {
    return rand() % n;
  }
}