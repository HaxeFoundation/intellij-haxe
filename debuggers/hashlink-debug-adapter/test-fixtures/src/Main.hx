/**
	Debuggee fixture entry point (Point/Config/Shade/Rich live in own files).

	WARNING: line numbers in this file are load-bearing test constants.
	The integration tests set breakpoints on the lines marked below and
	assert stack frames/values against them — do not reformat or reorder without
	updating DapIntegrationTestBase and the Haxe fixture-backed tests.

	The loop bound comes from a runtime value on purpose: a constant range like
	`0...3` is unrolled by the compiler, which would defeat a loop breakpoint.
**/
class Main {
	static function main():Void {
		Sys.println("fixture-start");
		var total = 0;
		var count = iterations();
		for (i in 0...count) {
			total = add(total, i); // FIXTURE_LOOP_LINE = 18
		}
		throwDemo(); inspectDemo(); Rich.demo(); Shadowed.demo(); Mutate.demo(); Call.demo(); ClosureCalls.demo(); slowDemo(); pkg.Deep.touch(); // same line: keeps the line constants below stable
		Sys.println("fixture-total:" + total);
	}

	static function iterations():Int {
		return Std.parseInt("3");
	}

	static function add(current:Int, amount:Int):Int {
		return current + amount; // FIXTURE_ADD_LINE = 29
	}

	static function inspectDemo():Void {
		var p = new Point(10, 20, "origin");
		var nums = [3, 5, 7];
		var v = Config.version; // FIXTURE_INSPECT_LINE = 35 (p and nums are in scope)
		Config.bump(); p.move(1, 2); // same line: keeps the line constants stable
		Sys.println("inspect:" + p.x + "," + nums[0] + "," + v);
	}

	// A call that takes noticeably long: stepping over it must WAIT for the
	// landing however long the call runs — never give up and resume freely.
	static function slowDemo():Void {
		var before = Std.parseInt("7") + 0; // plain Int 7
		Sys.sleep(slowSeconds()); // FIXTURE_SLOW_LINE = 44 — step over waits (3s when FIXTURE_SLOW=1)
		Sys.println("slow-done:" + before); // FIXTURE_SLOW_AFTER_LINE = 45
	}

	// A caught throw, so an "exception breakpoint" has a deterministic throw site
	// to stop on early in the run. Kept after slowDemo() so the line constants
	// above stay put; called first on the demo line so it fires before the sleep.
	static function throwDemo():Void {
		try {
			throw "boom";
		} catch (e:String) {
			Sys.println("caught:" + e);
		}
	}

	// The slow call's duration. main() runs slowDemo() unconditionally, so the
	// full 3s only happens for the step-over-a-slow-call test (FIXTURE_SLOW=1);
	// every other run-to-completion test was paying a flat 3s of pure sleep —
	// over half of a whole integration suite run. Added BELOW the other
	// functions so the load-bearing line constants above stay put.
	static function slowSeconds():Float {
		return Sys.getEnv("FIXTURE_SLOW") != null ? 3.0 : 0.05;
	}
}
