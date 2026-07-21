/**
	Debuggee fixture: compiled with `-lib intellij-hxcpp-debug-server -debug`
	(see fixture.hxml). Line numbers are load-bearing test constants — extend
	at the END of the file, never reflow.
**/
class Main {
	static function main():Void {
		Sys.println("fixture-start");
		var total = 0;
		for (i in 0...3) {
			total = add(total, i); // FIXTURE_LOOP_LINE = 11
		}
		Sys.println("fixture-total:" + total);
	}
	// called 3x; line 17 fires 3x (loop-body line 11 does not — see docs/README)
	static function add(current:Int, amount:Int):Int {
		return current + amount; // FIXTURE_ADD_LINE = 17
	}
}

// Evaluate-time mutation target (never called by the fixture itself): probes
// call Counter.bump() from an `evaluate` request to prove evaluated method
// calls execute real compiled code and their side effects persist in the
// debuggee. @:keep so no future dce flag strips the uncalled method.
@:keep
class Counter {
	public static var total = 0;

	public static function bump(amount:Int):Int {
		total += amount;
		return total;
	}
}
