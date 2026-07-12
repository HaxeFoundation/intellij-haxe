package tests;

import debug.eval.ExprAst.Expr;
import debug.eval.ExprParser;

/**
 * Parser structure tests: each expression is parsed and dumped to a canonical
 * fully-parenthesized string, which pins precedence and associativity (Haxe
 * rules: bitwise in one tier binding tighter than comparisons, shifts between
 * additive and bitwise, `/` just a normal multiplicative operator).
 */
class ExprParserTest {
	public static function run(assert:Assert):Void {
		function dump(e:Expr):String {
			return switch (e) {
				case EInt(v): haxe.Int64.toStr(v);
				case EFloat(f): Std.string(f);
				case EBool(b): b ? "true" : "false";
				case ENull: "null";
				case EString(s): '"' + s + '"';
				case EIdent(n): n;
				case EField(inner, n): dump(inner) + "." + n;
				case EIndex(inner, k): dump(inner) + "[" + dump(k) + "]";
				case ECall(inner, args): dump(inner) + "(" + [for (a in args) dump(a)].join(",") + ")";
				case ENew(cls, args): "new " + cls + "(" + [for (a in args) dump(a)].join(",") + ")";
				case EUnop(op, inner): "(" + op + dump(inner) + ")";
				case EBinop(op, l, r): "(" + dump(l) + op + dump(r) + ")";
				case EAssign(t, v): "(" + dump(t) + "=" + dump(v) + ")";
			}
		}
		function parsed(s:String):String {
			return dump(ExprParser.parse(s));
		}
		function rejects(s:String):Bool {
			return try {
				ExprParser.parse(s);
				false;
			} catch (e:debug.DebugError) true;
		}

		// literals
		assert.equals("42", parsed("42"), "int");
		assert.equals("255", parsed("0xFF"), "hex");
		assert.equals("2.5", parsed("2.5"), "float");
		assert.equals("true", parsed("true"), "true");
		assert.equals("null", parsed("null"), "null");
		assert.equals('"a\tb"', parsed('"a\\tb"'), "string escapes");

		// paths and postfix
		assert.equals("a.b.c", parsed("a.b.c"), "field chain");
		assert.equals("arr[0].label", parsed("arr[0].label"), "index in a chain");
		assert.equals("m[(i+1)]", parsed("m[i + 1]"), "computed key");
		assert.equals("f(1,x)", parsed("f(1, x)"), "call");
		assert.equals("obj.method(1)", parsed("obj.method(1)"), "method call");
		assert.equals("new Point(1,2)", parsed("new Point(1, 2)"), "new");
		assert.equals("new pkg.Deep(1)", parsed("new pkg.Deep(1)"), "packaged new");

		// precedence (Haxe)
		assert.equals("(1+(2*3))", parsed("1 + 2 * 3"), "mul over add");
		assert.equals("((1+2)*3)", parsed("(1 + 2) * 3"), "parens");
		assert.equals("(1<<(2+3))", parsed("1 << 2 + 3"), "shift under additive");
		assert.equals("((1|2)==3)", parsed("1 | 2 == 3"), "bitwise over comparison (Haxe)");
		assert.equals("((a<b)==c)", parsed("a < b == c"), "comparisons left-assoc, one tier");
		assert.equals("((a&&b)||c)", parsed("a && b || c"), "and over or");
		assert.equals("((a==1)&&(b==2))", parsed("a == 1 && b == 2"), "comparison over and");
		assert.equals("((-x)+1)", parsed("-x + 1"), "unary minus");
		assert.equals("(!(a==b))", parsed("!(a == b)"), "not");
		assert.equals("((1+2)-3)", parsed("1 + 2 - 3"), "additive left-assoc");
		assert.equals("((10/4)*2)", parsed("10 / 4 * 2"), "multiplicative left-assoc");
		assert.equals("(x=(y=1))", parsed("x = y = 1"), "assignment right-assoc");
		assert.equals("(n=((n*2)+1))", parsed("n = n * 2 + 1"), "assignment with expression RHS");
		assert.equals('("n="+n)', parsed('"n=" + n'), "string concat parse");
		assert.equals("((n>>>2)&15)", parsed("n >>> 2 & 15"), "ushr then bitwise");
		assert.equals("((~n)+1)", parsed("~n + 1"), "bitwise not");

		// errors
		assert.isTrue(rejects("1 +"), "dangling operator rejected");
		assert.isTrue(rejects('"unterminated'), "unterminated string rejected");
		assert.isTrue(rejects("f(1,"), "unclosed call rejected");
		assert.isTrue(rejects("a b"), "trailing garbage rejected");
		assert.isTrue(rejects("(1 + 2"), "unclosed paren rejected");
		assert.isTrue(rejects(""), "empty expression rejected");
	}
}
