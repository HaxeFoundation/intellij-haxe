/**
	Minimal assertion collector for the interpreter-run unit tests: counts
	checks and failures, prints each failure, and TestMain turns the totals
	into the exit code. Same shape as the other debugger modules' harnesses.
**/
class Assert {
	public var checks(default, null):Int = 0;
	public var failures(default, null):Int = 0;

	public function new() {}

	public function isTrue(condition:Bool, label:String):Void {
		checks++;
		if (!condition) {
			failures++;
			Sys.println("FAIL: " + label);
		}
	}

	public function equals(expected:Dynamic, actual:Dynamic, label:String):Void {
		isTrue(expected == actual, label + " (expected " + Std.string(expected) + ", got " + Std.string(actual) + ")");
	}
}
