/** An object with a @:packed field: the Vec2 struct is inlined into the instance (HPacked). */
class PackedHolder {
	public var id:Int;
	@:packed public var pos(default, null):Vec2;
	public var tail:Int;

	public function new(id:Int, x:Float, y:Float) {
		this.id = id;
		this.pos.x = x;
		this.pos.y = y;
		this.tail = id * 2;
	}
}
