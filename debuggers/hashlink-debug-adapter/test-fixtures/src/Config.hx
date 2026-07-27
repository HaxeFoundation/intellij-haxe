/**
	Statics fixture: version/title are read through the "Statics" scope.

	WARNING: line numbers are load-bearing test constants
	(FIXTURE_STATICS_LINE in DapIntegrationTestBase) — update them together.
**/
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

	// A function-typed static VAR (HFun at runtime, but NOT a method binding):
	// it must stay visible in the Statics scope and be resolvable as
	// `Config.onBump`, unlike the bump() method above which is hidden. Regression
	// guard: HFun-typed statics were previously dropped as if they were methods.
	// Kept after bump() so FIXTURE_STATICS_LINE stays put; @:keep survives DCE.
	@:keep public static var onBump:Null<Int->Void> = function(v:Int) {};
}
