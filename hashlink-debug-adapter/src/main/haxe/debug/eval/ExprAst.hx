package debug.eval;

/**
 * The evaluate-expression AST. Leaves are the constructs the debugger
 * already knows how to resolve (literals, variable paths, calls, `new`);
 * operators are folded ADAPTER-SIDE on typed values — no debuggee code runs
 * for arithmetic itself.
 */
enum Expr {
	EInt(v:haxe.Int64);
	EFloat(v:Float);
	EBool(v:Bool);
	ENull;
	EString(v:String);
	EIdent(name:String);
	/** `receiver.name` — a field access (or a class-path segment). */
	EField(e:Expr, name:String);
	/** `receiver[key]` — array index or map key (decided at eval time). */
	EIndex(e:Expr, key:Expr);
	/** `callee(args)` — callee must be an ident/field chain. */
	ECall(e:Expr, args:Array<Expr>);
	/** `new pkg.Cls(args)` */
	ENew(className:String, args:Array<Expr>);
	/** `!e`, `-e`, `~e` */
	EUnop(op:String, e:Expr);
	/** binary operator; `&&`/`||` short-circuit in the interpreter */
	EBinop(op:String, left:Expr, right:Expr);
	/** `cond ? thenExpr : elseExpr` — only the taken branch is evaluated */
	ETernary(cond:Expr, thenExpr:Expr, elseExpr:Expr);
	/** `e is Type` — a runtime type check; `typeName` is a (dotted) class/enum name */
	EIs(e:Expr, typeName:String);
	/** `target = value` (right-associative, lowest precedence) */
	EAssign(target:Expr, value:Expr);
}
