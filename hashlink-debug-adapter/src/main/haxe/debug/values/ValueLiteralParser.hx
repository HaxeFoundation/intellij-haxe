package debug.values;

import haxe.Int64;

/**
 * Parses the right-hand side of a value assignment into a ValueLiteral: a
 * literal for every primitive (decimal/hex int, float, true/false), `null`, or
 * a variable path whose existing value is copied. Anything else is null (the
 * caller reports a friendly error).
 */
class ValueLiteralParser {
	public static function parse(raw:Null<String>):Null<ValueLiteral> {
		if (raw == null) {
			return null;
		}
		var s = StringTools.trim(raw);
		if (s.length == 0) {
			return null;
		}
		switch (s) {
			case "null":
				return LNull;
			case "true":
				return LBool(true);
			case "false":
				return LBool(false);
			default:
		}
		var negative = false;
		var digits = s;
		if (StringTools.startsWith(digits, "-")) {
			negative = true;
			digits = StringTools.trim(digits.substr(1));
		}
		if (StringTools.startsWith(digits, "0x") || StringTools.startsWith(digits, "0X")) {
			var parsed:Int64 = Int64.ofInt(0);
			var any = false;
			for (i in 2...digits.length) {
				var c = StringTools.fastCodeAt(digits, i);
				var d = if (c >= "0".code && c <= "9".code) c - "0".code;
					else if (c >= "a".code && c <= "f".code) c - "a".code + 10;
					else if (c >= "A".code && c <= "F".code) c - "A".code + 10;
					else return null;
				parsed = Int64.add(Int64.shl(parsed, 4), Int64.ofInt(d));
				any = true;
			}
			if (!any) {
				return null;
			}
			return LInt(negative ? Int64.neg(parsed) : parsed);
		}
		if (startsWithDigit(digits)) {
			if (digits.indexOf(".") >= 0 || digits.indexOf("e") >= 0 || digits.indexOf("E") >= 0) {
				var f = Std.parseFloat(s);
				return Math.isNaN(f) ? null : LFloat(f);
			}
			var value = try Int64.parseString(digits) catch (e:Dynamic) null;
			if (value == null) {
				return null;
			}
			return LInt(negative ? Int64.neg(value) : value);
		}
		// not a numeric literal: a bare identifier path is a copy source
		if (negative) {
			return null; // "-name" is not a path
		}
		var path = ValuePath.parse(s);
		return path == null ? null : LPath(path);
	}

	static function startsWithDigit(s:String):Bool {
		if (s.length == 0) {
			return false;
		}
		var first = StringTools.fastCodeAt(s, 0);
		return first >= "0".code && first <= "9".code;
	}
}
