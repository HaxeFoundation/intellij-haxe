package debug.values;

/**
 * Parses a function-call expression `callee(arg, arg, ...)` where `callee` is a
 * variable path and the arguments are comma-separated sub-expressions. Returns
 * null when the text is not a call (no trailing argument list), so the caller
 * can fall through to assignment / plain-path handling.
 *
 * Commas inside nested parentheses or brackets are respected, so a path
 * argument like `arr[i]` or a (future) nested call survives the split. String
 * literals are NOT specially handled — arguments are literals or paths for now.
 */
class CallExpr {
	public final callee:String;
	public final args:Array<String>;
	// true for `new Class(args)` (construction) vs a plain `callee(args)` call
	public final isConstruction:Bool;

	function new(callee:String, args:Array<String>, isConstruction:Bool) {
		this.callee = callee;
		this.args = args;
		this.isConstruction = isConstruction;
	}

	public static function parse(expression:Null<String>):Null<CallExpr> {
		if (expression == null) {
			return null;
		}
		var s = StringTools.trim(expression);
		// `new Class(args)` — the class name is the callee, isConstruction marks it
		var construction = false;
		if (StringTools.startsWith(s, "new ")) {
			construction = true;
			s = StringTools.ltrim(s.substr(4));
		}
		if (s.length == 0 || s.charCodeAt(s.length - 1) != ")".code) {
			return null;
		}
		var open = s.indexOf("(");
		if (open <= 0) {
			return null; // no callee, or not a call
		}
		var callee = StringTools.trim(s.substring(0, open));
		// the callee must be a plain variable path; otherwise this is something
		// like `x = f(2)` — leave it to assignment handling
		if (callee.length == 0 || ValuePath.parse(callee) == null) {
			return null;
		}
		var inner = s.substring(open + 1, s.length - 1);
		return new CallExpr(callee, splitArgs(inner), construction);
	}

	// Splits on top-level commas (depth 0), honouring () and [] nesting. Empty
	// input is a zero-argument call.
	static function splitArgs(inner:String):Array<String> {
		var trimmed = StringTools.trim(inner);
		if (trimmed.length == 0) {
			return [];
		}
		var args:Array<String> = [];
		var depth = 0;
		var start = 0;
		for (i in 0...trimmed.length) {
			switch (StringTools.fastCodeAt(trimmed, i)) {
				case "(".code, "[".code:
					depth++;
				case ")".code, "]".code:
					depth--;
				case ",".code if (depth == 0):
					args.push(StringTools.trim(trimmed.substring(start, i)));
					start = i + 1;
				default:
			}
		}
		args.push(StringTools.trim(trimmed.substring(start)));
		return args;
	}
}
