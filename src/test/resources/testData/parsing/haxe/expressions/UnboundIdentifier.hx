class Main {
	static function main() {
		`trace("result", {fileName : "Main.hx", lineNumber : 11});
		var handler = `trace;
		if ((1 > 0)) `trace("big") else `trace("small");
	}
}
