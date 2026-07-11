/**
 * Debuggee fixture for the adapter integration tests.
 *
 * WARNING: line numbers in this file are load-bearing test constants.
 * The integration tests set breakpoints on the lines marked below and
 * assert stack frames/values against them — do not reformat or reorder without
 * updating the Java integration tests and the Haxe fixture-backed tests.
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
		inspectDemo(); Rich.demo(); // same line: keeps the line constants below stable
		Sys.println("fixture-total:" + total);
	}

	static function iterations():Int {
		return Std.parseInt("3");
	}

	static function add(current:Int, amount:Int):Int {
		return current + amount; // FIXTURE_ADD_LINE = 28
	}

	static function inspectDemo():Void {
		var p = new Point(10, 20, "origin");
		var nums = [3, 5, 7];
		var v = Config.version; // FIXTURE_INSPECT_LINE = 35 (p and nums are in scope)
		Config.bump();
		Sys.println("inspect:" + p.x + "," + nums[0] + "," + v);
	}
}

class Point {
	public var x:Int;
	public var y:Int;
	public var label:String;

	public function new(x:Int, y:Int, label:String) {
		this.x = x;
		this.y = y;
		this.label = label;
	}
}

class Config {
	public static var version:Int = 7;
	public static var title:String = "cfg";

	// A static method so a stopped frame's owning class (Config) has static fields
	// to show in the "Statics" scope. Breakpoint on the first line below.
	public static function bump():Int {
		var before = version; // FIXTURE_STATICS_LINE (version=7, title="cfg" in Statics)
		version = version + 1;
		return before;
	}
}

enum Shade {
	Plain;
	Tinted(level:Int, name:String);
}

class Rich {
	// One local per "rich" value kind, all built from runtime values so the
	// compiler cannot fold them away, and all used on the breakpoint line so
	// every register is live there.
	public static function demo():Void {
		var n:Int = Std.parseInt("2"); // typed Int so [n, ...] is ArrayBytes_Int, not boxed ArrayObj
		var ints = [n, n + 3, n * 5];
		var names = ["a" + n, "b"];
		var dyn:Dynamic = n + 40;
		var shade = Tinted(n, "red");
		var anon = {width: n, tag: "t" + n};
		var f = (a:Int, b:Int) -> a + b;
		Sys.println("rich:" + ints[n] + names[n - n] + Std.string(dyn) + Std.string(shade) + Std.string(anon) + f(n, n)); // FIXTURE_RICH_LINE (runtime indices so the arrays/anon can't be folded away)
	}
}
