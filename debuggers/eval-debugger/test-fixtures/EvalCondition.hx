/**
	Debuggee for the conditional-breakpoint tests: a counted loop whose body
	line carries a breakpoint with a condition on the counter. The bound is
	parsed at run time so nothing folds the loop away. Run with `haxe --interp`.

	WARNING: line numbers are load-bearing (EvalConditionalBreakpointLiveTest).
**/
class EvalCondition {
	static function main() {
		var total = 0;
		var count = Std.parseInt("6");
		for (i in 0...count) {
			total += i; // LOOP_BODY_LINE = 13
		}
		Sys.println("total:" + total); // AFTER_LOOP_LINE = 15
	}
}
