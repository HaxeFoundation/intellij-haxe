/**
	Minimal assertion collector for the adapter test suite.
	Kept dependency-free on purpose: no haxelib install is needed to run the tests.
**/
class Assert {
	public var failures(default, null):Int = 0;
	public var checks(default, null):Int = 0;

	var context:String = "";

	public function new() {}

	public function setContext(name:String):Void {
		context = name;
	}

	public function isTrue(condition:Bool, label:String):Void {
		checks++;
		if (!condition) {
			report(label + ": expected true");
		}
	}

	public function isFalse(condition:Bool, label:String):Void {
		checks++;
		if (condition) {
			report(label + ": expected false");
		}
	}

	public function equals(expected:Dynamic, actual:Dynamic, label:String):Void {
		checks++;
		if (expected != actual) {
			report(label + ": expected <" + Std.string(expected) + "> but got <" + Std.string(actual) + ">");
		}
	}

	public function fail(label:String):Void {
		checks++;
		report(label);
	}

	function report(message:String):Void {
		failures++;
		Sys.println("FAIL [" + context + "] " + message);
	}
}
