/**
 * Uncaught-exception fixture: a CAUGHT throw (which the "uncaught" filter must
 * skip) followed by an UNCAUGHT throw (which it must stop on). The uncaught throw
 * would terminate the program, so a test stops at it and disconnects rather than
 * continuing.
 */
class Uncaught {
	public static function main():Void {
		Sys.println("uncaught-start");
		try {
			throw "caught-one";
		} catch (e:String) {
			Sys.println("caught:" + e);
		}
		throw "uncaught-one"; // no surrounding try/catch — uncaught
	}
}
