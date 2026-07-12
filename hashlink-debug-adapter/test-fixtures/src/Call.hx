/**
 * Eval-call fixture (M13): unbound function values the debugger can invoke via
 * `evaluate` while stopped. Each local is a reference to a static method (an
 * unbound closure — no captured environment), kept alive as a real escaping
 * value through `keep` so the analyzer can't inline or eliminate it.
 *
 * WARNING: FIXTURE_CALL_LINE in DapIntegrationTestBase is the breakpoint line
 * below — keep them in sync.
 */
class Call {
	static function addImpl(a:Int, b:Int):Int {
		return a + b;
	}

	static function scaleImpl(x:Float):Float {
		return x * 2.5;
	}

	static function negateImpl(b:Bool):Bool {
		return !b;
	}

	public static function demo():Void {
		var base:Int = Std.parseInt("10") + 0; // plain Int (10), not Null<Int>
		var add:(Int, Int) -> Int = addImpl;
		var scale:(Float) -> Float = scaleImpl;
		var negate:(Bool) -> Bool = negateImpl;
		var keep:Array<Dynamic> = [add, scale, negate]; // force them to be real locals
		Sys.println("call:" + add(base, 1) + "," + scale(base * 1.0) + "," + negate(base < 0) + "," + keep.length); // FIXTURE_CALL_LINE = 28
	}
}
