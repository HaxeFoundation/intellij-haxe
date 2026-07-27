/**
	Type-filtered exception breakpoint fixture: throws two different
	exception types. With a filter on "Boom", the unrelated haxe.Exception throw
	must be SKIPPED, and only the Kaboom throw (a Boom subclass — this exercises
	subtype matching) must stop the debugger. Both are caught so the program would
	run to completion if nothing matched.
**/
class TypedThrow {
	public static function main():Void {
		Sys.println("typed-start");
		try {
			throw new haxe.Exception("other-one"); // not a Boom — the filter must skip this
		} catch (e:haxe.Exception) {
			Sys.println("caught-other");
		}
		try {
			throw new Kaboom("kaboom-one"); // Kaboom extends Boom — filter "Boom" stops here
		} catch (e:haxe.Exception) {
			Sys.println("caught-kaboom");
		}
		Sys.println("typed-done");
	}
}
