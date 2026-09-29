class Main {
	static function main() {
		var count = 3;
		if (count > 0) trace("positive");
		if (count > 1) trace("big");
		else
			trace("small");
		if (count > 2) {
			trace("block");
		}
		while (count > 0)
			count--;
		trace(count);
	}
}
