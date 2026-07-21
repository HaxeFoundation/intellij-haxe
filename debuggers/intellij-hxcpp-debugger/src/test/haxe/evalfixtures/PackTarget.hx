package evalfixtures;

/**
	Packaged sibling of the root-package `EvalTarget`: exercises the dotted
	type-path binding in `ResolvingInterp` (`evalfixtures.PackTarget.bump(4)`).
**/
class PackTarget {
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
