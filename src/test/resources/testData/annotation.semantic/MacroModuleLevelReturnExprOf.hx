package;

#if macro
import haxe.macro.Expr;
#end

// Module-level macro functions with an ExprOf<String> return tag (and, for `nbuild`, an
// ExprOf<Int> parameter) whose `haxe.macro.Expr` import is guarded by `#if macro`. Returning
// a `macro` reification of a String-typed expression must NOT be flagged
// "Incompatible type: ExprOf<String> should be ExprOf<String>", and neither the return nor
// the parameter `ExprOf` may be reported as an unresolved type/symbol.
macro function build(value:String):ExprOf<String> {
  return macro Std.string($v{value});
}

macro function nbuild(a:String, b:String, num:ExprOf<Int>):ExprOf<String> {
  return macro Std.string($v{a});
}
