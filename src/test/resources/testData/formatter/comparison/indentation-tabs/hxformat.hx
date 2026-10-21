class Main {
	static function main() {
		var total = 0;
		for (i in 0...5) {
			if (i % 2 == 0) {
				total += i;
			}
		}
		switch (total) {
			case 0:
				trace("zero");
			case n if (n > 4):
				trace("big");
			default:
				trace(total);
		}
	}
}
