package evalfixtures.deep;

/**
	Two package levels deep: proves the type-path binder walks arbitrary-length
	dotted prefixes (`evalfixtures.deep.DeepTarget.bump(3)`).
**/
class DeepTarget {
	public static var total = 0;

	public static function bump(amount:Int):Int {
		total += amount;
		return total;
	}
}
