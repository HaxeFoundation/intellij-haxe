package debug.eval;

import debug.DebugError;
import debug.eval.EvalValue;
import debug.values.ValueReader;
import haxe.Int64;

/**
 * Pure adapter-side operator folding over EvalValue (M21b) — Haxe semantics:
 *
 * - `/` is ALWAYS Float (like Haxe, unlike C).
 * - `+` concatenates when either side is a String (numbers/bools/null are
 *   stringified; raw objects refuse — we won't silently call toString).
 * - `%` and the other arithmetic ops stay Int when both sides are Int.
 * - bitwise ops and shifts use 32-bit Int semantics (HL's Int is 32-bit).
 * - `==`/`!=` compare strings by CONTENT, objects by pointer; mismatched
 *   kinds are simply not equal (never an error).
 * - `< <= > >=` work on numbers and on strings (lexicographic).
 *
 * `&&`/`||` are NOT here: they short-circuit in the interpreter.
 */
class Operators {
	public static function binop(op:String, a:EvalValue, b:EvalValue):EvalValue {
		return switch (op) {
			case "+": add(a, b);
			case "-": arith(op, a, b, (x, y) -> Int64.sub(x, y), (x, y) -> x - y);
			case "*": arith(op, a, b, (x, y) -> Int64.mul(x, y), (x, y) -> x * y);
			case "%": modulo(a, b);
			case "/": VFloat(asFloat(a, op) / asFloat(b, op)); // Haxe: division is always Float
			case "&": VInt(Int64.ofInt(asInt32(a, op) & asInt32(b, op)));
			case "|": VInt(Int64.ofInt(asInt32(a, op) | asInt32(b, op)));
			case "^": VInt(Int64.ofInt(asInt32(a, op) ^ asInt32(b, op)));
			case "<<": VInt(Int64.ofInt(asInt32(a, op) << asInt32(b, op)));
			case ">>": VInt(Int64.ofInt(asInt32(a, op) >> asInt32(b, op)));
			case ">>>": VInt(Int64.ofInt(asInt32(a, op) >>> asInt32(b, op)));
			case "==": VBool(equals(a, b));
			case "!=": VBool(!equals(a, b));
			case "<", "<=", ">", ">=": VBool(ordered(op, a, b));
			default: throw new DebugError('Unsupported operator "' + op + '"');
		}
	}

	public static function unop(op:String, a:EvalValue):EvalValue {
		return switch (op) {
			case "-":
				switch (a) {
					case VInt(v): VInt(Int64.neg(v));
					case VFloat(f): VFloat(-f);
					default: throw new DebugError("Unary '-' needs a number");
				}
			case "!":
				VBool(!asBool(a, "!"));
			case "~":
				VInt(Int64.ofInt(~asInt32(a, "~")));
			default:
				throw new DebugError('Unsupported unary operator "' + op + '"');
		}
	}

	/** Bool coercion for logical operators and conditions. */
	public static function asBool(v:EvalValue, op:String):Bool {
		return switch (v) {
			case VBool(b): b;
			default: throw new DebugError("'" + op + "' needs a Bool operand, got " + describe(v));
		}
	}

	/** Haxe-ish Std.string for concat results and messages. */
	public static function stringify(v:EvalValue):String {
		return switch (v) {
			case VInt(i): Int64.toStr(i);
			case VFloat(f): Std.string(f);
			case VBool(b): b ? "true" : "false";
			case VString(s, _): s;
			case VNull: "null";
			case VObject(_, _): throw new DebugError("Cannot stringify an object here"
				+ " — call its toString() in the expression instead");
		}
	}

	// --- internals ---

	static function add(a:EvalValue, b:EvalValue):EvalValue {
		if (a.match(VString(_, _)) || b.match(VString(_, _))) {
			return VString(stringify(a) + stringify(b), null);
		}
		return arith("+", a, b, (x, y) -> Int64.add(x, y), (x, y) -> x + y);
	}

	static function arith(op:String, a:EvalValue, b:EvalValue,
			ints:(Int64, Int64) -> Int64, floats:(Float, Float) -> Float):EvalValue {
		return switch [a, b] {
			case [VInt(x), VInt(y)]: VInt(ints(x, y));
			case [VInt(_) | VFloat(_), VInt(_) | VFloat(_)]: VFloat(floats(asFloat(a, op), asFloat(b, op)));
			default: throw new DebugError("'" + op + "' needs numbers, got " + describe(a) + " and " + describe(b));
		}
	}

	static function modulo(a:EvalValue, b:EvalValue):EvalValue {
		return switch [a, b] {
			case [VInt(x), VInt(y)]:
				if (Int64.eq(y, Int64.ofInt(0))) {
					throw new DebugError("Modulo by zero");
				}
				VInt(Int64.mod(x, y));
			case [VInt(_) | VFloat(_), VInt(_) | VFloat(_)]:
				VFloat(asFloat(a, "%") % asFloat(b, "%"));
			default:
				throw new DebugError("'%' needs numbers, got " + describe(a) + " and " + describe(b));
		}
	}

	static function equals(a:EvalValue, b:EvalValue):Bool {
		return switch [a, b] {
			case [VNull, VNull]: true;
			case [VNull, VObject(raw, _)] | [VObject(raw, _), VNull]: Int64.eq(raw, Int64.ofInt(0));
			case [VNull, VString(_, ptr)] | [VString(_, ptr), VNull]: ptr != null && Int64.eq(ptr, Int64.ofInt(0));
			case [VInt(x), VInt(y)]: Int64.eq(x, y);
			case [VInt(_) | VFloat(_), VInt(_) | VFloat(_)]: asFloat(a, "==") == asFloat(b, "==");
			case [VBool(x), VBool(y)]: x == y;
			case [VString(x, _), VString(y, _)]: x == y; // content, like Haxe
			case [VObject(x, _), VObject(y, _)]: Int64.eq(x, y); // pointer identity
			default: false; // mismatched kinds: not equal, never an error
		}
	}

	static function ordered(op:String, a:EvalValue, b:EvalValue):Bool {
		switch [a, b] {
			case [VString(x, _), VString(y, _)]:
				var c = x < y ? -1 : (x > y ? 1 : 0);
				return applyOrder(op, c);
			case [VInt(x), VInt(y)]:
				var c = Int64.compare(x, y);
				return applyOrder(op, c);
			case [VInt(_) | VFloat(_), VInt(_) | VFloat(_)]:
				var x = asFloat(a, op);
				var y = asFloat(b, op);
				var c = x < y ? -1 : (x > y ? 1 : 0);
				return applyOrder(op, c);
			default:
				throw new DebugError("'" + op + "' needs two numbers or two strings, got "
					+ describe(a) + " and " + describe(b));
		}
	}

	static function applyOrder(op:String, c:Int):Bool {
		return switch (op) {
			case "<": c < 0;
			case "<=": c <= 0;
			case ">": c > 0;
			default: c >= 0; // ">="
		}
	}

	static function asFloat(v:EvalValue, op:String):Float {
		return switch (v) {
			case VInt(i): Int64.toInt(i); // safe: HL ints are 32-bit
			case VFloat(f): f;
			default: throw new DebugError("'" + op + "' needs a number, got " + describe(v));
		}
	}

	static function asInt32(v:EvalValue, op:String):Int {
		return switch (v) {
			case VInt(i): Int64.toInt(i);
			default: throw new DebugError("'" + op + "' needs an Int, got " + describe(v));
		}
	}

	public static function describe(v:EvalValue):String {
		return switch (v) {
			case VInt(_): "an Int";
			case VFloat(_): "a Float";
			case VBool(_): "a Bool";
			case VString(_, _): "a String";
			case VNull: "null";
			case VObject(_, t): "an object (" + ValueReader.typeName(t) + ")";
		}
	}
}
