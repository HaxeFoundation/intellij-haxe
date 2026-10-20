class Main {
	static function greet(name:String):String {
		return "hello " + name;
	}

	static function main() {
		var who = greet("world");
		if (who.length > 0) {
			trace(who);
		}
		while (false) {}
		for (i in 0...2) {
			trace(i);
		}
	}
}
