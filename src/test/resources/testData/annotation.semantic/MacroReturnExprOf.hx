package;

import haxe.macro.Expr;

// A macro function whose declared return type is ExprOf<String> and whose body
// returns a `macro` reification of a String-typed expression. The reification is
// typed as ExprOf<String> too, so the return-type check compares ExprOf<String>
// against ExprOf<String> and must NOT report an incompatible-type error.
class MacroReturnExprOf {
  macro public static function build(value:String):ExprOf<String> {
    return macro Std.string($v{value});
  }
}
