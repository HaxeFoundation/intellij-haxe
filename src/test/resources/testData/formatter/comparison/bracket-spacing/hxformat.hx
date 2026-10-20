class Main {
	static function main() {
		var values = [1, 2, 3];
		var lookup = ["one" => 1, "two" => 2];
		var first = values[0];
		var second = values[1];
		var doubled = [for (v in values) v * 2];
		trace(first + second + doubled[0] + lookup["one"]);
	}
}
