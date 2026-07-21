/**
	Debuggee for the "breakpoints win over steps" tests: a breakpoint parked
	early in `work` must still be honoured by step-out (bp further down the
	same function) and by step-over of a call whose body holds a breakpoint.
	Run with `haxe --interp`.

	WARNING: line numbers are load-bearing (EvalStepBreakpointLiveTest).
**/
class EvalStepBp {
	static function helper() {
		Sys.println("h1"); // HELPER_BP_LINE = 11
		Sys.println("h2");
	}

	static function work() {
		Sys.println("w1"); // WORK_START_LINE = 16
		Sys.println("w2");
		helper(); // HELPER_CALL_LINE = 18
		Sys.println("w3"); // WORK_LATER_BP_LINE = 19
		Sys.println("w4");
	}

	static function main() {
		work(); // MAIN_CALL_LINE = 24
		Sys.println("done"); // MAIN_AFTER_LINE = 25
	}
}
