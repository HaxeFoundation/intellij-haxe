package;

class Main {
	static function main() {
		// up to 80 columns of items: kept on one line, written breaks included
		var short = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
		var shortBroken = [
			"alpha", "bravo", "charlie", "delta", "echo"
		];

		// over 80 columns of items and four or more of them: one per line
		var manyItems = ["alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliet"];
		var manyItemsBroken = ["alpha", "bravo", "charlie", "delta", "echo", "foxtrot",
			"golf", "hotel", "india", "juliet"];

		// over 80 columns in three items, one of them 30 columns or more: one per line
		var longItem = ["a rather long first item that passes thirty", "second item is also somewhat long", "third item text"];

		// over 80 columns in three items, none reaching 30 with its separator: kept, the line fits
		var threeMediumItems = ["first medium sized items", "second medium sized item", "third medium sized items"];

		// ten or more items of up to 10 columns each: a leading break, then the line fills
		var tinyItems = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390, 400];

		// ten or more items of one length up to 30 columns each: a leading break, then the line fills
		var equalItems = ["item number 01", "item number 02", "item number 03", "item number 04", "item number 05", "item number 06", "item number 07", "item number 08", "item number 09", "item number 10"];

		// an item written over several lines: one per line
		var withFunctions = [function() {
			trace("one");
		}, function() {
			trace("two");
		}];
	}
}
