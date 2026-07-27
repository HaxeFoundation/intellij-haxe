/**
	Debuggee throwing an UNCAUGHT haxe.Exception INSTANCE (not a bare string):
	the constructor and the uncaught-printer run real, steppable Haxe std code,
	which behaves differently under the debugger than a string throw.
	Run with `haxe --interp`.

	WARNING: line numbers are load-bearing (live tests).
**/
class EvalThrowObj {
	static function boom() {
		throw new haxe.Exception("uncaught-object"); // THROW_LINE = 11
	}

	static function main() {
		Sys.println("before-throw");
		boom(); // CALL_LINE = 16
		Sys.println("after-throw");
	}
}
