/**
	Debuggee for the eval live tests, run with `haxe --interp`.

	WARNING: line numbers are load-bearing test constants
	(EvalLiveTest / EvalDebugAdapterLiveTest) — extend at the END, never reflow.
**/
class EvalMain {
	static function main() {
		var greeting = "hello";
		var count = greeting.length + 2; // BREAK_LINE = 10
		var nested = outer(inner(3)); // NESTED_CALL_LINE = 11
		Sys.println("eval-fixture:" + greeting + ":" + count + ":" + nested); chain(); Coll.collections(); // same line: keeps constants below stable
	}

	static function inner(x:Int):Int {
		return x * 2; // INNER_LINE = 16 (first executable line eval stops on)
	}

	static function outer(x:Int):Int {
		return x + 1;
	}

	static function chain() {
		var cfg = new Chain();
		cfg.test1(1).test2().test3().test1(2); // CHAIN_LINE = 25
		Sys.println("chain:" + cfg.sum); // CHAIN_AFTER_LINE = 26
	}
}

class Chain {
	public var sum = 0;
	public function new() {}
	public function test1(x:Int):Chain {
		sum += x; // TEST1_LINE = 34
		return this;
	}
	public function test2():Chain {
		sum += 10; // TEST2_LINE = 38
		return this;
	}
	public function test3():Chain {
		sum += 100; // TEST3_LINE = 42
		return this;
	}
}

class Coll {
	public static function collections() {
		var items = make(); // via a call, or the analyzer scalar-replaces the array
		var chain = new Chain();
		Sys.println("coll:" + items[1] + ":" + chain.sum); // COLL_LINE = 51
	}
	static function make():Array<Int> {
		return [10, 20, 30];
	}
}
