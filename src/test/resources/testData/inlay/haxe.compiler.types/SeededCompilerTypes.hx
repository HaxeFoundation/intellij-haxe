package testData.inlay.haxe.compiler.types;
enum Opt {
  Some(v:Int);
  None;
}
class SeededCompilerTypes {
  static var count/*<# :|Int #>*/ = 1;
  static var typed:Int = 2;
  static function run(size/*<# :|Int #>*/, total:Int)/*<# :|String #>*/ {
    var sum/*<# :|Int #>*/ = size + total;
    var typedLocal:Int = sum;
    function local()/*<# :|Bool #>*/ { return true; }
    for (item/*<# :|String #>*/ in ["a"]) {}
    for (k/*<# :|String #>*/ => val/*<# :|Int #>*/ in ["a" => 1]) {}
    switch (Some(sum)) {
      case Some(v/*<# :|Int #>*/): trace(v);
      case None:
    }
    return "";
  }
  public function new() {}
}
