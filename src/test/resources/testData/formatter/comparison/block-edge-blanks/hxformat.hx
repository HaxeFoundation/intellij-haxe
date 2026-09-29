class Main {
	static function main() {
		var total = 0;

		if (total == 0) {
			total++;
		}

		while (total < 3) {
			total++;
		}

		switch (total) {
			case 3:
				trace("three");

			default:
				trace(total);
		}

		var point = {
			x: total,
			y: 2
		};

		trace(point.x + point.y);
	}
}
