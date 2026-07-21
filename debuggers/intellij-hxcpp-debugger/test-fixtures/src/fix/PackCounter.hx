package fix;

/**
	Packaged evaluate-time mutation target (never called by the fixture
	itself; compiled in via an explicit module line in fixture.hxml): probes
	call `fix.PackCounter.bump()` from an `evaluate` request to prove dotted
	type paths resolve against the live debuggee.
**/
@:keep
class PackCounter {
	public static var total = 0;

	public static function bump(amount:Int):Int {
		total += amount;
		return total;
	}
}
