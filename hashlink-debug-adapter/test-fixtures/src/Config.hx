/**
 * Statics fixture: version/title are read through the "Statics" scope.
 *
 * WARNING: line numbers are load-bearing test constants
 * (FIXTURE_STATICS_LINE in DapIntegrationTestBase) — update them together.
 */
class Config {
	public static var version:Int = 7;
	public static var title:String = "cfg";

	// A static method so a stopped frame's owning class (Config) has static
	// fields to show in the "Statics" scope. Breakpoint on the line below.
	public static function bump():Int {
		var before = version; // FIXTURE_STATICS_LINE = 14 (version=7, title="cfg")
		version = version + 1;
		return before;
	}
}
