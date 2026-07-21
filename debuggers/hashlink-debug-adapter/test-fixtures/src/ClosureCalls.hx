/**
	Closure-call step-into fixture: the callee is only knowable at RUNTIME (a
	function stored in a variable), so step-into must resolve the closure's
	`fun` pointer at the stop to enter it — there is no static call target in
	the bytecode (OCallClosure).

	NOTE the two array flavours: `functions[0]()` with a CONSTANT index is
	folded away entirely by the analyzer (the closure lands directly in a
	register, no array exists at runtime), while `callbacks[idx]()` with a
	RUNTIME index forces a real ArrayObj — the shape real programs have, where
	the closure register only loads MID-line (the deferred-resolution path)
	and the array itself is inspectable in the variables view.

	WARNING: FIXTURE_CLOSURE_* in DapIntegrationTestBase are the marked lines
	below — keep them in sync.
**/
class ClosureCalls {
	public static function demo():Void {
		var holder = new Holder(1);
		Sys.println("closure:" + holder.value);
	}
}

class Holder {
	public var value:Int;

	public function new(v:Int) {
		var fn = grab;
		var got = fn(); // FIXTURE_CLOSURE_CALL_LINE = 29
		var functions = [grab];
		got += functions[0](); // FIXTURE_CLOSURE_ARRAY_CALL_LINE = 31 (the analyzer FOLDS this array away)
		var callbacks = [grab, fn];
		var idx = Std.parseInt("0"); // runtime index: `callbacks` must stay a REAL array
		got += callbacks[idx](); // FIXTURE_CLOSURE_REAL_ARRAY_LINE = 34 (the closure register loads ON this line)
		value = got + v;
	}

	public function grab():Int {
		var a = 40; // FIXTURE_CLOSURE_BODY_LINE = 39 (the step-in landing)
		var b = a + 1; // FIXTURE_CLOSURE_BODY_LINE2 = 40 (a NEXT from line 39 lands here, never the caller)
		return b;
	}
}
