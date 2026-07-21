/**
	VM-raised exception fixture: a genuine null access, which HashLink's C
	runtime raises through hl_null_access -> hl_throw WITHOUT executing a
	bytecode OThrow. The OThrow-based exception breakpoints never see it; only
	the hl_throw trap (the "vm" filter) can stop here — and the stop must carry
	the actual runtime message ("Null access .length") read from the thread's
	exc_value at hl_throw's own debug break.

	`maybe` is null via a runtime value so the compiler cannot fold the access
	away, and it stays a named local so a test can inspect it (== null) at the
	stop. The access is uncaught, so a test stops at it and disconnects rather
	than continuing (continuing lets the throw terminate the program).
**/
class VmError {
	public static function main():Void {
		Sys.println("vm-error-start");
		var maybe:Array<Int> = makeNull();
		var n = maybe.length; // bp:nullaccess  (maybe is null -> VM null access)
		Sys.println("unreachable " + n);
	}

	// Returns null, but through a runtime value so the analyzer can't prove it
	// and fold the null access into a compile-time result.
	static function makeNull():Array<Int> {
		return Std.parseInt("0") == 0 ? null : [1];
	}
}
