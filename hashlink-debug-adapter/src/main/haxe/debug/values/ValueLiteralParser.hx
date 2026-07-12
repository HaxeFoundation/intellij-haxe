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
		if (s.length >= 2 && s.charCodeAt(0) == "\"".code && s.charCodeAt(s.length - 1) == "\"".code) {
			var text = unescape(s.substring(1, s.length - 1));
			return text == null ? null : LString(text);
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

	// Unescapes the inside of a "..." literal (\n \t \\ \" \0). Returns null on a
	// dangling or unknown escape, or an inner unescaped quote (which would mean
	// the outer quotes weren't the string bounds).
	static function unescape(inner:String):Null<String> {
		var out = new StringBuf();
		var i = 0;
		while (i < inner.length) {
			var c = StringTools.fastCodeAt(inner, i);
			if (c == "\"".code) {
				return null; // an unescaped quote inside the bounds
			}
			if (c != "\\".code) {
				out.addChar(c);
				i++;
				continue;
			}
			if (i + 1 >= inner.length) {
				return null;
			}
			switch (StringTools.fastCodeAt(inner, i + 1)) {
				case "n".code: out.addChar("\n".code);
				case "t".code: out.addChar("\t".code);
				case "r".code: out.addChar("\r".code);
				case "\\".code: out.addChar("\\".code);
				case "\"".code: out.addChar("\"".code);
				case "0".code: out.addChar(0);
				default: return null;
			}
			i += 2;
		}
		return out.toString();
	}

	static function startsWithDigit(s:String):Bool {
		if (s.length == 0) {
			return false;
		}
		var first = StringTools.fastCodeAt(s, 0);
		return first >= "0".code && first <= "9".code;
	}
}
