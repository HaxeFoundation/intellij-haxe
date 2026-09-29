class Main {
	static function sum(a:Int, b:Int, c:Int):Int {
		return a + b + c;
	}

	static function main() {
		var numbers = [1, 2, 3, 4];
		var lookup = new Map<Int, String>();
		lookup.set(1, "one");
		trace(sum(1, 2, 3), numbers, lookup);
	}
}
