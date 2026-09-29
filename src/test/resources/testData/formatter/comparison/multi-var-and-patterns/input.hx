class Main {
	@:isVar public var count(get, set):Int;

	static var first:Int = 1,second:Int = 2,  third:Int = 3;

	static function pick(v:Int):String {
		var a = 1,b = 2,c = 3;
		var w = untyped __js__("window");
		trace(w + a + b + c);
		return switch (v) {
			case -1 | -2 | -3: "small";
			case n if (n < -10): "verysmall";
			default: "negative";
		};
	}

	static function main() {
		trace(pick(1) + first + second + third);
	}

	static function get_count():Int return 0;

	static function set_count(v:Int):Int return v;
}
