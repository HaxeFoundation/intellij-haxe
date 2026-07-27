/**
	Real compiled class the Evaluator tests call INTO from evaluated
	expressions, proving hscript executes actual program code and mutates
	actual program state (it is reflection over live references, not a
	sandbox). Root package on purpose: `ResolvingInterp` resolves bare
	identifiers only.
**/
class EvalTarget {
	public static var total = 0;

	public static function bump(amount:Int):Int {
		total += amount;
		return total;
	}

	public var value:Int;

	public function new(value:Int) {
		this.value = value;
	}

	public function addTo(amount:Int):Int {
		value += amount;
		return value;
	}
}
