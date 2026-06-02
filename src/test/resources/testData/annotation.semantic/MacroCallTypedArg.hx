package;

import haxe.macro.Expr;

// Calling a macro function that declares a concrete `:String` parameter and an
// ExprOf<String> return. From the caller's side this is a 1-argument call returning
// String; it must NOT be flagged "Too many arguments" or a type mismatch.
class Test {
  macro public static function build(value:String):ExprOf<String> {
    return macro Std.string($v{value});
  }

  public static function use() {
    build("hello");
    var s:String = build("world");
  }
}
