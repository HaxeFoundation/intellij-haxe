/**
 * Debuggee fixture for the adapter integration tests.
 *
 * WARNING: line numbers in this file are load-bearing test constants.
 * The integration tests set breakpoints on the lines marked below and
 * assert stack frames against them — do not reformat or reorder without
 * updating DebugLifecycleIntegrationTest (Java) and ModuleDebugInfoTest (Haxe).
 */
class Main {
	static function main():Void {
		Sys.println("fixture-start");
		var total = 0;
		for (i in 0...3) {
			total = add(total, i); // FIXTURE_LOOP_LINE = 14
		}
		Sys.println("fixture-total:" + total);
	}

	static function add(current:Int, amount:Int):Int {
		return current + amount; // FIXTURE_ADD_LINE = 20
	}
}
