class Main {
	static function main() {
		var x = 2;
		if (x > 0) {
			trace("positive");
		} else if (x < 0) {
			trace("negative");
		} else {
			trace("zero");
		}
		try {
			throw "boom";
		} catch (e:String) {
			trace(e);
		}
		do {
			x--;
		} while (x > 0);
	}
}
