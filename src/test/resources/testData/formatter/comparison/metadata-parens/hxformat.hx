@:keep
@:native("ext") class Main {
	@:isVar public var count(get, set):Int;

	@:pure(true)
	static function main() {
		trace("ok");
	}

	static function get_count():Int {
		return 0;
	}

	static function set_count(v:Int):Int {
		return v;
	}
}
