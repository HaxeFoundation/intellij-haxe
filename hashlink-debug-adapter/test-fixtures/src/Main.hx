/**
 * Debuggee fixture for the adapter integration tests.
 *
 * WARNING: line numbers in this file are load-bearing test constants.
 * The integration tests set breakpoints on the lines marked below and
 * assert stack frames against them — do not reformat or reorder without
 * updating DebugLifecycleIntegrationTest (Java) and ModuleDebugInfoTest (Haxe).
 *
 * The loop bound comes from a runtime value on purpose: a constant range like
 * `0...3` is unrolled by the compiler, which would defeat a loop breakpoint.
 */
class Main {
	static function main():Void {
		Sys.println("fixture-start");
		var total = 0;
		var count = iterations();
		for (i in 0...count) {
			total = add(total, i); // FIXTURE_LOOP_LINE = 18
		}
		Sys.println("fixture-total:" + total);
	}

	static function iterations():Int {
		return Std.parseInt("3"); // runtime value keeps the loop from being unrolled
	}

	static function add(current:Int, amount:Int):Int {
		return current + amount; // FIXTURE_ADD_LINE = 27
	}
}
