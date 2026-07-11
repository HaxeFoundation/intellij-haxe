/**
 * Shadowing fixture: an outer String `x` is shadowed by the for-loop Int `x`.
 * Inside the loop exactly one `x` (the Int) must be listed; after the loop the
 * name must fall back to the String — and no ghost row may keep tracking the
 * recycled loop register.
 *
 * WARNING: line numbers are load-bearing test constants
 * (FIXTURE_SHADOW_* in DapIntegrationTestBase) — update them together.
 */
class Shadowed {
	public static function demo():Void {
		var n = Std.parseInt("3");
		var x = "outer" + n;
		var total = 0;
		for (x in 0...n) {
			total += x; // FIXTURE_SHADOW_LOOP_LINE = 16
		}
		Sys.println("shadow:" + x + total); // FIXTURE_SHADOW_AFTER_LINE = 18
	}
}
