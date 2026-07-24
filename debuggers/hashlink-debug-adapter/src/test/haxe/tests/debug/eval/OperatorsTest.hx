package tests.debug.eval;

import ijhaxe.debug.eval.EvalValue;
import ijhaxe.debug.eval.Operators;
import haxe.Int64;

/**
	Pure operator-fold tests: Haxe semantics over EvalValue.
**/
class OperatorsTest {
	public static function run(assert:Assert):Void {
		inline function i(v:Int):EvalValue
			return VInt(Int64.ofInt(v));
		inline function f(v:Float):EvalValue
			return VFloat(v);
		inline function s(v:String):EvalValue
			return VString(v, null);
		function throws(run:() -> Void):Bool {
			return try {
				run();
				false;
			} catch (e:ijhaxe.debug.DebugError) true;
		}

		// arithmetic + promotion
		assert.equals("7", dump(Operators.binop("+", i(3), i(4))), "int add stays int");
		assert.equals("3.5", dump(Operators.binop("+", i(3), f(0.5))), "int+float promotes");
		assert.equals("2.5", dump(Operators.binop("/", i(5), i(2))), "division is ALWAYS Float (Haxe)");
		assert.equals("15", dump(Operators.binop("*", i(3), i(5))), "int mul");
		assert.equals("1", dump(Operators.binop("%", i(7), i(3))), "int mod");
		assert.equals("-5", dump(Operators.unop("-", i(5))), "unary minus");

		// string concat (either side)
		assert.equals("n=5", dump(Operators.binop("+", s("n="), i(5))), "string + int");
		assert.equals("5n", dump(Operators.binop("+", i(5), s("n"))), "int + string");
		assert.equals("x=null", dump(Operators.binop("+", s("x="), VNull)), "string + null");
		assert.equals("f=0.5", dump(Operators.binop("+", s("f="), f(0.5))), "string + float");

		// bitwise / shifts (32-bit int semantics)
		assert.equals("20", dump(Operators.binop("<<", i(5), i(2))), "shl");
		assert.equals("1", dump(Operators.binop(">>", i(5), i(2))), "shr");
		assert.equals("4", dump(Operators.binop("&", i(5), i(6))), "and");
		assert.equals("7", dump(Operators.binop("|", i(5), i(6))), "or");
		assert.equals("3", dump(Operators.binop("^", i(5), i(6))), "xor");
		assert.equals("-6", dump(Operators.unop("~", i(5))), "bitwise not");
		assert.equals("2147483647", dump(Operators.binop(">>>", i(-1), i(1))), "ushr is unsigned");

		// comparisons
		assert.equals("true", dump(Operators.binop("==", i(5), i(5))), "int eq");
		assert.equals("true", dump(Operators.binop("==", i(5), f(5.0))), "int/float eq promotes");
		assert.equals("true", dump(Operators.binop("==", s("abc"), s("abc"))), "string CONTENT eq");
		assert.equals("false", dump(Operators.binop("==", s("abc"), s("abd"))), "string neq content");
		assert.equals("true", dump(Operators.binop("<", s("abc"), s("abd"))), "string ordering");
		assert.equals("true", dump(Operators.binop("!=", i(1), s("1"))), "mismatched kinds are not equal");
		assert.equals("true", dump(Operators.binop("==", VNull, VNull)), "null eq null");
		assert.equals("true", dump(Operators.binop("<=", i(5), i(5))), "lte");
		assert.equals("false", dump(Operators.binop(">", i(2), i(5))), "gt");

		// unary logic
		assert.equals("false", dump(Operators.unop("!", VBool(true))), "not");

		// errors are clear, not silent garbage
		assert.isTrue(throws(() -> Operators.binop("%", i(1), i(0))), "modulo by zero throws");
		assert.isTrue(throws(() -> Operators.binop("-", s("a"), i(1))), "string minus throws");
		assert.isTrue(throws(() -> Operators.binop("<", VBool(true), i(1))), "bool ordering throws");
		assert.isTrue(throws(() -> Operators.unop("-", s("a"))), "unary minus on string throws");
		assert.isTrue(throws(() -> Operators.asBool(i(1), "&&")), "&& on int throws");
	}

	static function dump(v:EvalValue):String {
		return switch (v) {
			case VInt(i): Int64.toStr(i);
			case VFloat(f): Std.string(f);
			case VBool(b): b ? "true" : "false";
			case VString(s, _): s;
			case VNull: "null";
			case VObject(_, _): "<object>";
		}
	}
}
