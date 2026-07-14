/**
 * Stack-trace presentation fixture: builds a haxe.Exception a couple of
 * calls deep — which captures __nativeStack as an hl.NativeArray<hl_symbol> — and
 * holds it in a local so a test can stop, expand the exception, and confirm each
 * __nativeStack entry is resolved to a "Class.method (File.hx:line)" label instead
 * of an opaque `hl_symbol @ 0x..` pointer.
 */
class StackTrace {
	public static function main():Void {
		var err = build();
		Sys.println("built:" + err.message); // BREAKPOINT: `err` is a live local here
	}

	static function build():haxe.Exception {
		return deeper();
	}

	static function deeper():haxe.Exception {
		return new haxe.Exception("captured-here"); // __nativeStack captured on this line
	}
}
