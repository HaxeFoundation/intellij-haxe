class Main {
	static function longReturn(flag:Bool):String {
		if (flag)
			return "a very long return value that we keep on one line";
		return
			"broken return written by hand";
	}

	static function bare(v:Int):Void {
		if (v == 0)
			return;
		trace(v);
	}

	static function main() {
		trace(longReturn(true));
		bare(0);
	}
}
