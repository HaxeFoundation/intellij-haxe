class Main {
	static function main() {
		var text = "The quick brown fox jumps over the lazy dog and keeps on running until the very end of the line";
		var words = text.toLowerCase()
			.split(" ")
			.map(word -> word.charAt(0).toUpperCase() + word.substr(1))
			.filter(word -> word.length > 3)
			.map(word -> word + "!")
			.join(" ");
		trace(words);
	}
}
