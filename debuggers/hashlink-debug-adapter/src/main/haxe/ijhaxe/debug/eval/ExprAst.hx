package ijhaxe.debug.eval;

/**
	The syntax tree of an evaluate expression. Leaves are the constructs the
	debugger can already resolve: literals, variable paths, calls and `new`.
	Operators are computed inside the adapter on typed values, so no debuggee
	code runs for arithmetic.
**/
enum Expr {
	EInt(v:haxe.Int64);
	EFloat(v:Float);
	EBool(v:Bool);
	ENull;
	EString(v:String);
	EIdent(name:String);

	/**
		`receiver.name`: a field access, or one segment of a class path.
	**/
	EField(e:Expr, name:String);

	/**
		`receiver[key]`: an array index or a map key, decided during evaluation.
	**/
	EIndex(e:Expr, key:Expr);

	/**
		`callee(args)`, where the callee must be a variable path.
	**/
	ECall(e:Expr, args:Array<Expr>);

	/**
		`new pkg.Cls(args)`
	**/
	ENew(className:String, args:Array<Expr>);

	/**
		`!e`, `-e`, `~e`
	**/
	EUnop(op:String, e:Expr);

	/**
		A binary operator. The interpreter short-circuits `&&` and `||`.
	**/
	EBinop(op:String, left:Expr, right:Expr);

	/**
		`cond ? thenExpr : elseExpr`. Only the taken branch is evaluated.
	**/
	ETernary(cond:Expr, thenExpr:Expr, elseExpr:Expr);

	/**
		`e is Type`: a runtime type check against a (dotted) class or enum name.
	**/
	EIs(e:Expr, typeName:String);

	/**
		`target = value` (right-associative, lowest precedence)
	**/
	EAssign(target:Expr, value:Expr);
}
