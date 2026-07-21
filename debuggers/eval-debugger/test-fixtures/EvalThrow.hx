/**
	Debuggee that throws an UNCAUGHT exception, for the exception-stop tests.
	Run with `haxe --interp`.

	WARNING: line numbers are load-bearing (EvalDebugAdapterLiveTest).
**/
class EvalThrow {
	static function boom() {
		throw "uncaught-boom"; // THROW_LINE = 9
	}

	static function middle() {
		var local = "in-middle";
		boom(); // deepens the unwind chain the exception propagates through
		Sys.println(local);
	}

	static function main() {
		Sys.println("before-throw");
		middle();
		Sys.println("after-throw");
	}
}
