/**
 * Value-modification fixture. The test stops at the checkpoint, writes new
 * values into locals/fields/elements, then resumes: the printed result and the
 * taken branch prove the writes took effect and steered execution.
 *
 * All locals come from runtime values so the analyzer can't fold them (a
 * constant `flag = false` would delete the branch; a constant index would fold
 * the element read).
 *
 * WARNING: FIXTURE_MUTATE_LINE in DapIntegrationTestBase is the checkpoint
 * line below — keep them in sync.
 */
class Mutate {
	public static function demo():Void {
		var seed = Std.parseInt("5"); // Null<Int>, defeats the analyzer
		var n:Int = seed + 0; // plain Int (5)
		var flag = n < 0; // runtime false; set true at the checkpoint to take the branch
		var obj = new Point(1, 2, "p");
		var arr = [n, n * 2, n * 3];
		var idx:Int = seed - 4; // plain Int (1)
		Sys.println("mutate-checkpoint"); // FIXTURE_MUTATE_LINE = 21
		if (flag) {
			Sys.println("mutate-branch-taken");
		}
		Sys.println("mutate-result:" + n + "," + obj.x + "," + arr[idx]);
		cachedUse(n);
	}

	// Probes the JIT register-cache behavior: `v` is loaded on the first line
	// and used again on the second with NO call between. A debugger write to
	// v's stack slot while stopped on the second line reveals whether the
	// compiled code re-reads the slot or uses a cached CPU register.
	static function cachedUse(v:Int):Void {
		var doubled = v * 2;
		Sys.println("cached:" + (v + doubled)); // FIXTURE_CACHED_LINE = 35
	}
}
