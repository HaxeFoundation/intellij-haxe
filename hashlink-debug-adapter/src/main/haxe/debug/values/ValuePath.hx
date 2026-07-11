package debug.values;

/**
 * Parses the variable-path subset the evaluator supports:
 *
 *   path     = ident accessor*
 *   accessor = "." ident | "[" digits "]"
 *   ident    = [A-Za-z_$][A-Za-z0-9_]*
 *
 * Anything else (operators, calls, string literals, ...) is NOT an evaluable
 * path and parses to null — the caller reports a friendly "paths only" error.
 */
class ValuePath {
	public final root:String;
	public final accessors:Array<PathAccessor>;

	function new(root:String, accessors:Array<PathAccessor>) {
		this.root = root;
		this.accessors = accessors;
	}

	public static function parse(expression:Null<String>):Null<ValuePath> {
		if (expression == null) {
			return null;
		}
		var s = StringTools.trim(expression);
		if (s.length == 0) {
			return null;
		}
		var pos = 0;

		inline function peek():Int {
			return pos < s.length ? StringTools.fastCodeAt(s, pos) : -1;
		}

		function isIdentStart(c:Int):Bool {
			return (c >= "a".code && c <= "z".code) || (c >= "A".code && c <= "Z".code) || c == "_".code || c == "$".code;
		}
		function isIdentPart(c:Int):Bool {
			return isIdentStart(c) || (c >= "0".code && c <= "9".code);
		}
		function readIdent():Null<String> {
			if (!isIdentStart(peek())) {
				return null;
			}
			var start = pos;
			while (isIdentPart(peek())) {
				pos++;
			}
			return s.substring(start, pos);
		}

		var root = readIdent();
		if (root == null) {
			return null;
		}
		var accessors:Array<PathAccessor> = [];
		while (pos < s.length) {
			switch (peek()) {
				case ".".code:
					pos++;
					var name = readIdent();
					if (name == null) {
						return null;
					}
					accessors.push(Field(name));
				case "[".code:
					pos++;
					var start = pos;
					while (peek() >= "0".code && peek() <= "9".code) {
						pos++;
					}
					if (pos == start || peek() != "]".code) {
						return null;
					}
					accessors.push(Index(Std.parseInt(s.substring(start, pos))));
					pos++;
				default:
					return null; // operators, whitespace inside the path, calls, ...
			}
		}
		return new ValuePath(root, accessors);
	}
}
