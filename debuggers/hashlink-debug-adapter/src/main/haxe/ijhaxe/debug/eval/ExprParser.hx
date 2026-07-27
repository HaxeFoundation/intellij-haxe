package ijhaxe.debug.eval;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.eval.ExprAst.Expr;

/**
	Tokenizer + precedence-climbing parser for evaluate expressions.

	Operator precedence follows HAXE (which differs from C: bitwise ops share
	one tier and bind tighter than comparisons; shifts sit between additive and
	bitwise):

	| tier | operators                          | notes                       |
	|------|------------------------------------|-----------------------------|
	| 1    | unary `!` `-` `~`                  | prefix                      |
	| 2    | `%` `*` `/`                        |                             |
	| 3    | `+` `-`                            |                             |
	| 4    | `<<` `>>` `>>>`                    |                             |
	| 5    | `&` `\|` `^`                       | one tier, left-associative  |
	| 6    | `==` `!=` `<` `<=` `>` `>=` `is`   |                             |
	| 7    | `&&`                               |                             |
	| 8    | `\|\|`                             |                             |
	| 9    | `?:`                               | ternary, right-associative  |
	| 10   | `=`                                | right-associative           |

	Postfix: `.field`, `[expr]`, `(args)`. Primary: literals, identifiers,
	parentheses, `new pkg.Cls(args)`. `e is Type` sits at the comparison level;
	its right side is a (dotted) type name, not an expression.
**/
class ExprParser {
	/**
		Parses a full expression; throws DebugError with a clear message.
	**/
	public static function parse(text:String):Expr {
		var parser = new ExprParser(text);
		var e = parser.parseAssign();
		parser.skipWhitespace();
		if (parser.pos < parser.text.length) {
			throw new DebugError('Unexpected "' + parser.text.substr(parser.pos) + '" after the expression');
		}
		return e;
	}

	final text:String;
	var pos:Int = 0;

	function new(text:String) {
		this.text = text;
	}

	// --- precedence levels (lowest first) ---

	function parseAssign():Expr {
		var left = parseTernary();
		skipWhitespace();
		if (peekOp("=") && !peekOp("==")) {
			pos++;
			return EAssign(left, parseAssign()); // right-associative
		}
		return left;
	}

	function parseTernary():Expr {
		var cond = parseOr();
		skipWhitespace();
		if (peekOp("?")) {
			pos++;
			var thenExpr = parseAssign(); // between ? and : anything is allowed
			skipWhitespace();
			expect(":");
			var elseExpr = parseAssign(); // right-assoc: a?b:c?d:e = a?b:(c?d:e)
			return ETernary(cond, thenExpr, elseExpr);
		}
		return cond;
	}

	function parseOr():Expr {
		var left = parseAnd();
		while (true) {
			skipWhitespace();
			if (peekOp("||")) {
				pos += 2;
				left = EBinop("||", left, parseAnd());
			} else {
				return left;
			}
		}
	}

	function parseAnd():Expr {
		var left = parseCompare();
		while (true) {
			skipWhitespace();
			if (peekOp("&&")) {
				pos += 2;
				left = EBinop("&&", left, parseCompare());
			} else {
				return left;
			}
		}
	}

	function parseCompare():Expr {
		var left = parseBitwise();
		while (true) {
			skipWhitespace();
			// `e is Type` — the right operand is a (dotted) type name, not an expr
			if (peekKeyword("is")) {
				pos += 2;
				left = EIs(left, readTypePath());
				continue;
			}
			var op = matchFirst(["==", "!=", "<=", ">=", "<", ">"]);
			// `<` / `>` must not swallow the first char of `<<` / `>>`
			if (op == "<" && peekOp("<<")) {
				return left;
			}
			if (op == ">" && (peekOp(">>") || peekOp(">>>"))) {
				return left;
			}
			if (op == null) {
				return left;
			}
			pos += op.length;
			left = EBinop(op, left, parseBitwise());
		}
	}

	// A dotted type name after `is`, `new`, etc. (`Point`, `pkg.sub.Cls`).
	function readTypePath():String {
		var name = readIdent("a type name");
		while (true) {
			skipWhitespace();
			if (peekOp(".")) {
				pos++;
				skipWhitespace();
				name += "." + readIdent("a type-name segment after '.'");
			} else {
				return name;
			}
		}
	}

	// True when the identifier `word` sits at the cursor as a whole token (so
	// `is` matches but `isReady` does not).
	function peekKeyword(word:String):Bool {
		if (!peekOp(word)) {
			return false;
		}
		var after = pos + word.length;
		return after >= text.length || !isIdentPart(StringTools.fastCodeAt(text, after));
	}

	function parseBitwise():Expr {
		var left = parseShift();
		while (true) {
			skipWhitespace();
			// single & or | only (never the start of && / ||)
			if (peekOp("&") && !peekOp("&&")) {
				pos++;
				left = EBinop("&", left, parseShift());
			} else if (peekOp("|") && !peekOp("||")) {
				pos++;
				left = EBinop("|", left, parseShift());
			} else if (peekOp("^")) {
				pos++;
				left = EBinop("^", left, parseShift());
			} else {
				return left;
			}
		}
	}

	function parseShift():Expr {
		var left = parseAdditive();
		while (true) {
			skipWhitespace();
			if (peekOp(">>>")) {
				pos += 3;
				left = EBinop(">>>", left, parseAdditive());
			} else if (peekOp("<<")) {
				pos += 2;
				left = EBinop("<<", left, parseAdditive());
			} else if (peekOp(">>") && !peekOp(">>>")) {
				pos += 2;
				left = EBinop(">>", left, parseAdditive());
			} else {
				return left;
			}
		}
	}

	function parseAdditive():Expr {
		var left = parseMultiplicative();
		while (true) {
			skipWhitespace();
			if (peekOp("+")) {
				pos++;
				left = EBinop("+", left, parseMultiplicative());
			} else if (peekOp("-")) {
				pos++;
				left = EBinop("-", left, parseMultiplicative());
			} else {
				return left;
			}
		}
	}

	function parseMultiplicative():Expr {
		var left = parseUnary();
		while (true) {
			skipWhitespace();
			if (peekOp("*")) {
				pos++;
				left = EBinop("*", left, parseUnary());
			} else if (peekOp("/")) {
				pos++;
				left = EBinop("/", left, parseUnary());
			} else if (peekOp("%")) {
				pos++;
				left = EBinop("%", left, parseUnary());
			} else {
				return left;
			}
		}
	}

	function parseUnary():Expr {
		skipWhitespace();
		if (peekOp("!") && !peekOp("!=")) {
			pos++;
			return EUnop("!", parseUnary());
		}
		if (peekOp("-")) {
			pos++;
			return EUnop("-", parseUnary());
		}
		if (peekOp("~")) {
			pos++;
			return EUnop("~", parseUnary());
		}
		return parsePostfix();
	}

	function parsePostfix():Expr {
		var e = parsePrimary();
		while (true) {
			skipWhitespace();
			if (peekOp(".")) {
				pos++;
				skipWhitespace();
				e = EField(e, readIdent("a field name after '.'"));
			} else if (peekOp("[")) {
				pos++;
				var key = parseAssign();
				expect("]");
				e = EIndex(e, key);
			} else if (peekOp("(")) {
				pos++;
				e = ECall(e, parseArgs());
			} else {
				return e;
			}
		}
	}

	// arguments after a consumed "(" up to the matching ")"
	function parseArgs():Array<Expr> {
		var args:Array<Expr> = [];
		skipWhitespace();
		if (peekOp(")")) {
			pos++;
			return args;
		}
		while (true) {
			args.push(parseAssign());
			skipWhitespace();
			if (peekOp(",")) {
				pos++;
			} else {
				expect(")");
				return args;
			}
		}
	}

	function parsePrimary():Expr {
		skipWhitespace();
		if (pos >= text.length) {
			throw new DebugError("Unexpected end of expression");
		}
		var c = StringTools.fastCodeAt(text, pos);
		if (c == "(".code) {
			pos++;
			var e = parseAssign();
			expect(")");
			return e;
		}
		if (c == '"'.code) {
			return EString(readStringLiteral());
		}
		if (c >= "0".code && c <= "9".code) {
			return readNumber();
		}
		if (isIdentStart(c)) {
			var ident = readIdent("a name");
			return switch (ident) {
				case "true": EBool(true);
				case "false": EBool(false);
				case "null": ENull;
				case "new": readNew();
				default: EIdent(ident);
			}
		}
		throw new DebugError('Unexpected character "' + String.fromCharCode(c) + '" in the expression');
	}

	// `new pkg.Cls(args)` — the keyword was already consumed
	function readNew():Expr {
		skipWhitespace();
		var name = readIdent("a class name after 'new'");
		while (true) {
			skipWhitespace();
			// a dot ALWAYS extends the class name here: `new a.b.Cls(...)`
			if (peekOp(".")) {
				pos++;
				skipWhitespace();
				name += "." + readIdent("a class-name segment after '.'");
			} else {
				break;
			}
		}
		skipWhitespace();
		expect("(");
		return ENew(name, parseArgs());
	}

	// --- tokens ---

	function readNumber():Expr {
		var start = pos;
		if (StringTools.fastCodeAt(text, pos) == "0".code && pos + 1 < text.length
			&& (StringTools.fastCodeAt(text, pos + 1) == "x".code || StringTools.fastCodeAt(text, pos + 1) == "X".code)) {
			pos += 2;
			var hexStart = pos;
			while (pos < text.length && isHexDigit(StringTools.fastCodeAt(text, pos))) {
				pos++;
			}
			if (pos == hexStart) {
				throw new DebugError("Malformed hex number");
			}
			return EInt(parseHex(text.substring(hexStart, pos)));
		}
		while (pos < text.length && isDigit(StringTools.fastCodeAt(text, pos))) {
			pos++;
		}
		var isFloat = false;
		// a '.' only continues the number when a digit follows (so `1.field` — not
		// valid Haxe anyway — never mis-tokenizes and `arr[1].x` stays a path)
		if (pos + 1 < text.length && StringTools.fastCodeAt(text, pos) == ".".code
			&& isDigit(StringTools.fastCodeAt(text, pos + 1))) {
			isFloat = true;
			pos++;
			while (pos < text.length && isDigit(StringTools.fastCodeAt(text, pos))) {
				pos++;
			}
		}
		var raw = text.substring(start, pos);
		if (isFloat) {
			var f = Std.parseFloat(raw);
			if (Math.isNaN(f)) {
				throw new DebugError('Malformed number "' + raw + '"');
			}
			return EFloat(f);
		}
		return EInt(haxe.Int64.parseString(raw));
	}

	static function parseHex(digits:String):haxe.Int64 {
		var value = haxe.Int64.ofInt(0);
		for (i in 0...digits.length) {
			var c = StringTools.fastCodeAt(digits, i);
			var digit = if (c >= "0".code && c <= "9".code) c - "0".code
				else if (c >= "a".code && c <= "f".code) c - "a".code + 10
				else c - "A".code + 10;
			value = haxe.Int64.add(haxe.Int64.shl(value, 4), haxe.Int64.ofInt(digit));
		}
		return value;
	}

	function readStringLiteral():String {
		pos++; // opening quote
		var buf = new StringBuf();
		while (pos < text.length) {
			var c = StringTools.fastCodeAt(text, pos);
			if (c == '"'.code) {
				pos++;
				return buf.toString();
			}
			if (c == "\\".code) {
				pos++;
				if (pos >= text.length) {
					break;
				}
				var esc = StringTools.fastCodeAt(text, pos);
				switch (esc) {
					case "n".code: buf.addChar(10);
					case "t".code: buf.addChar(9);
					case "r".code: buf.addChar(13);
					case "0".code: buf.addChar(0);
					case "\\".code: buf.addChar("\\".code);
					case '"'.code: buf.addChar('"'.code);
					default: throw new DebugError('Unknown string escape "\\' + String.fromCharCode(esc) + '"');
				}
				pos++;
			} else {
				buf.addChar(c);
				pos++;
			}
		}
		throw new DebugError("Unterminated string literal");
	}

	function readIdent(what:String):String {
		skipWhitespace();
		if (pos >= text.length || !isIdentStart(StringTools.fastCodeAt(text, pos))) {
			throw new DebugError("Expected " + what);
		}
		var start = pos;
		pos++;
		while (pos < text.length && isIdentPart(StringTools.fastCodeAt(text, pos))) {
			pos++;
		}
		return text.substring(start, pos);
	}

	// --- low-level helpers ---

	function expect(punct:String):Void {
		skipWhitespace();
		if (!peekOp(punct)) {
			throw new DebugError('Expected "' + punct + '"');
		}
		pos += punct.length;
	}

	function peekOp(op:String):Bool {
		if (pos + op.length > text.length) {
			return false;
		}
		for (i in 0...op.length) {
			if (StringTools.fastCodeAt(text, pos + i) != StringTools.fastCodeAt(op, i)) {
				return false;
			}
		}
		return true;
	}

	// the first of `ops` present at the cursor (longer options listed first)
	function matchFirst(ops:Array<String>):Null<String> {
		for (op in ops) {
			if (peekOp(op)) {
				return op;
			}
		}
		return null;
	}

	function skipWhitespace():Void {
		while (pos < text.length) {
			var c = StringTools.fastCodeAt(text, pos);
			if (c == " ".code || c == "\t".code || c == "\n".code || c == "\r".code) {
				pos++;
			} else {
				break;
			}
		}
	}

	static inline function isDigit(c:Int):Bool {
		return c >= "0".code && c <= "9".code;
	}

	static inline function isHexDigit(c:Int):Bool {
		return isDigit(c) || (c >= "a".code && c <= "f".code) || (c >= "A".code && c <= "F".code);
	}

	static inline function isIdentStart(c:Int):Bool {
		return (c >= "a".code && c <= "z".code) || (c >= "A".code && c <= "Z".code) || c == "_".code || c == "$".code;
	}

	static inline function isIdentPart(c:Int):Bool {
		return isIdentStart(c) || isDigit(c);
	}
}
