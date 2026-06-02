package;

#if macro
import haxe.macro.Expr;
#end

// Class-based macro function with a `#if macro` guarded import. Used to isolate
// whether the conditional import alone triggers the false positive.
class MacroClassConditionalImport {
  macro public static function build(value:String):ExprOf<String> {
    return macro Std.string($v{value});
  }
}
