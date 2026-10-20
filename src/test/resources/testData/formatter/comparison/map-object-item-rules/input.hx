package;

class Main {
	static function main() {
		// maps follow the array rules: up to 80 columns of entries stay on one line, written breaks removed
		var shortMap = [1 => "one", 2 => "two", 3 => "three"];
		var shortMapBroken = [
			1 => "one",
			2 => "two"
		];

		// four or more entries over 80 columns: one per line
		var manyEntries = ["alpha" => 1, "bravo" => 2, "charlie" => 3, "delta" => 4, "echo" => 5, "foxtrot" => 6];

		// an entry of 30 columns or more: one per line
		var longEntry = ["a rather long key that passes thirty" => 1, "second key" => 2, "third key is a bit longer" => 3];

		// ten or more entries of up to 10 columns each: a leading break, then the line fills
		var tinyEntries = [1 => 1, 2 => 2, 3 => 3, 4 => 4, 5 => 5, 6 => 6, 7 => 7, 8 => 8, 9 => 9, 10 => 10, 11 => 11, 12 => 12, 13 => 13, 14 => 14, 15 => 15, 16 => 16, 17 => 17, 18 => 18, 19 => 19, 20 => 20, 21 => 21, 22 => 22, 23 => 23, 24 => 24, 25 => 25, 26 => 26, 27 => 27, 28 => 28, 29 => 29, 30 => 30];

		// objects written on one line: up to three fields on a fitting line stay, however long; one written over lines goes one per line
		var point = {x: 1, y: 2};
		var pointBroken = {x: 1,
			y: 2, z: 3};
		var described = {alpha: "some text here", bravo: "some text there", charlie: "more text here"};

		// four or more fields: one per line
		var rect = {x: 1, y: 2, width: 3, height: 4};
		var rectBroken = {
			x: 1, y: 2,
			width: 3, height: 4
		};

		// a field of 30 columns or more counts only past three fields: two stay
		var named = {name: "a rather long value passing thirty", id: 1};

		trace(shortMap, shortMapBroken, manyEntries, longEntry, tinyEntries, point, pointBroken, described, rect, rectBroken, named);
	}
}
