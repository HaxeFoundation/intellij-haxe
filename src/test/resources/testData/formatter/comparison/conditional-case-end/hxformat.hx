class Main {
	static function main() {
		var kind = 1;
		switch (kind) {
			case 1:
				#if js
				trace("js");
				if (kind > 0) {
					trace("positive");
				}
				#else
				trace("other");
				#end
			case 2:
				trace("two");
			#if debug
			case 3:
				trace("three");
			#end
			default:
		}
	}
}
