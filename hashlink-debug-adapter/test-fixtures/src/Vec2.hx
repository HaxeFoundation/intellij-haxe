/** A flat value class (HStruct on HashLink): no hl_type* header, fields at base 0. */
@:struct class Vec2 {
	public var x:Float;
	public var y:Float;

	public function new(x:Float, y:Float) {
		this.x = x;
		this.y = y;
	}
}
