/**
 * Object-expansion target for the variables tests: a Point(10, 20, "origin")
 * built in Main.inspectDemo is expanded field by field.
 */
class Point {
	public var x:Int;
	public var y:Int;
	public var label:String;

	public function new(x:Int, y:Int, label:String) {
		this.x = x;
		this.y = y;
		this.label = label;
	}
}
