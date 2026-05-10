package;

import haxe.macro.Context;
import haxe.macro.Expr;

class Test {

  public function test():ExprOf<String> {
    //OK
    var a:ExprOf<String> = {expr: EConst(CString(Std.string(buildNumber))), pos : Context.currentPos()};

    // WRONG
    var <error descr="Incompatible type: missing member(s) expr:ExprDef">a:ExprOf<String> = {expression: EConst(CString(Std.string(buildNumber))), pos : Context.currentPos()}</error>;

    // WRONG Incorrect const type (experimental check)
    var b:ExprOf<String> = {expr: EConst(<error descr="have 'CInt' wants 'CString'">CInt(Std.string(buildNumber))</error>), pos : Context.currentPos()};

    // verify return type check also works
    return {expr: EConst(CString(Std.string(buildNumber))), pos : Context.currentPos()};
  }
}