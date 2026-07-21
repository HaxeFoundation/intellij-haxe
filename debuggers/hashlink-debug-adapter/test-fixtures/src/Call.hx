/**
	Eval-call fixture: unbound function values the debugger can invoke via
	`evaluate` while stopped, plus a String local to reassign from a call result,
	plus BOUND closures: an instance-method closure (captured value = the
	receiver object) and a capturing lambda (captured value = the environment).
	Each closure local is kept alive as a real escaping value through `keep`.

	WARNING: FIXTURE_CALL_LINE in DapIntegrationTestBase is the breakpoint line
	below — keep them in sync.
**/
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

	static function makeLabelImpl(n:Int):String {
		return "L" + n;
	}

	final bonus:Int;

	function new(bonus:Int) {
		this.bonus = bonus;
	}

	function boost(n:Int):Int {
		return n + bonus;
	}

	public static function demo():Void {
		var base:Int = Std.parseInt("10") + 0; // plain Int (10), not Null<Int>
		var add:(Int, Int) -> Int = addImpl;
		var scale:(Float) -> Float = scaleImpl;
		var negate:(Bool) -> Bool = negateImpl;
		var label:(Int) -> String = makeLabelImpl;
		var s = "orig" + base; // a String local reassigned from a call result
		var inst = new Call(base + 5); // bonus = 15
		var boost:(Int) -> Int = inst.boost; // bound method closure: value = inst
		var plus = (n:Int) -> n + base + 2; // capturing lambda: value = capture env
		// NOTE: no haxe.io.Bytes keepalive — the debugger now builds strings via
		// the low-level `alloc_bytes` native, so string creation must work even
		// when the program never uses haxe.io.Bytes (the reported case).
		var keep:Array<Dynamic> = [add, scale, negate, label, boost, plus]; // force real locals
		Sys.println("call:" + add(base, 1) + "," + scale(base * 1.0) + "," + negate(base < 0) + "," + s + "," + label(base) + "," + boost(1) + "," + plus(1) + "," + keep.length); // FIXTURE_CALL_LINE = 52
	}
}
