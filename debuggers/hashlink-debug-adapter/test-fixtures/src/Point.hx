/**
	Object-expansion target for the variables tests: a Point(10, 20, "origin")
	built in Main.inspectDemo is expanded field by field.

	WARNING: line numbers are load-bearing test constants
	(FIXTURE_POINT_METHOD_LINE in DapIntegrationTestBase) — update together.
**/
class Point {
	public var x:Int;
	public var y:Int;
	public var label:String;

	public function new(x:Int, y:Int, label:String) {
		this.x = x;
		this.y = y;
		this.label = label;
	}

	// An instance-method frame, so the tests can assert `this` shows up in
	// Locals. Breakpoint on the line below (this.x is still 10 there).
	public function move(dx:Int, dy:Int):Void {
		x = x + dx; // FIXTURE_POINT_METHOD_LINE = 22
		y = y + dy;
	}

	// static data (declared after move so the line constant stays stable):
	// the Statics scope must appear inside instance-method frames too
	public static var axes:Int = 2;
}
