package;

class ArrayWrapChecks {
	static function main() {
		// KEEP: items total at most 80 columns (each counted with its ", "): one line, written breaks removed
		var shortList = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
		var shortListBroken = ["alpha", "bravo", "charlie", "delta", "echo"];

		// ONE PER LINE: four or more items totalling over 80
		var manyItems = [
			"alpha",
			"bravo",
			"charlie",
			"delta",
			"echo",
			"foxtrot",
			"golf",
			"hotel",
			"india",
			"juliet"
		];
		var manyItemsBroken = [
			"alpha",
			"bravo",
			"charlie",
			"delta",
			"echo",
			"foxtrot",
			"golf",
			"hotel",
			"india",
			"juliet"
		];

		// ONE PER LINE: an item of 30 columns or more (with its ", "), the total over 80
		var longItem = [
			"a rather long first item that passes thirty",
			"second item is also somewhat long",
			"third item text"
		];

		// KEEP: three items over 80 in total, none reaching 30, the line fits the margin
		var threeMediumItems = ["first medium sized items", "second medium sized item", "third medium sized items"];

		// ONE PER LINE: the same shape but the line passes the 160 column margin (the overflow rule)
		var aVeryLongVariableNameThatPushesTheWholeLiteralWellPastTheMargin = [
			"first medium sized items",
			"second medium sized item",
			"third medium sized items"
		];

		// FILL AFTER A LEADING BREAK: ten or more items of one length up to 30
		var equalItems = [
			"item number 01", "item number 02", "item number 03", "item number 04", "item number 05", "item number 06", "item number 07", "item number 08",
			"item number 09", "item number 10"
		];
		var equalItemsBroken = [
			"item number 01", "item number 02", "item number 03", "item number 04", "item number 05", "item number 06", "item number 07", "item number 08",
			"item number 09", "item number 10"
		];

		// FILL AFTER A LEADING BREAK: ten or more items of up to 10 columns each, mixed lengths
		var tinyItems = [
			10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310,
			320, 330, 340, 350, 360, 370, 380, 390, 400
		];

		// ONE PER LINE: an item written over several lines, however short the list
		var withFunctions = [
			function() {
				trace("one");
			},
			function() {
				trace("two");
			}
		];

		// the rules apply per literal: a nested array is judged on its own
		var nested = [
			[
				"alpha",
				"bravo",
				"charlie",
				"delta",
				"echo",
				"foxtrot",
				"golf",
				"hotel",
				"india",
				"juliet"
			],
			["x", "y"]
		];

		trace(shortList.length
			+ shortListBroken.length
			+ manyItems.length
			+ manyItemsBroken.length
			+ longItem.length
			+ threeMediumItems.length);
		trace(aVeryLongVariableNameThatPushesTheWholeLiteralWellPastTheMargin.length
			+ equalItems.length
			+ equalItemsBroken.length
			+ tinyItems.length);
		trace(withFunctions.length + nested.length);
	}
}
