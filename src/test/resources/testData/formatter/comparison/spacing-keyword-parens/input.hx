class Main {
	static function main() {
		var x = 1;
		if(x > 0) {
			trace("positive");
		}
		while(x < 10) {
			x++;
		}
		for(i in 0...3) {
			trace(i);
		}
		switch(x) {
			case 1:
				trace("one");
			default:
				trace("other");
		}
		try {
			throw "boom";
		} catch(e:String) {
			trace(e);
		}
		do {
			x--;
		} while(x > 5);
	}
}
