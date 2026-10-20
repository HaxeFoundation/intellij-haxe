class Main {
	static function noop() {}

	static function main() {
		if (true) {}
		while (false) {}
		var anon = function() {};
		var arrow = () -> {};
		noop();
		anon();
		arrow();
	}
}
