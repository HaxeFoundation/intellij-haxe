/**
	An object with a @:packed field: the Vec2 struct is inlined into the instance (HPacked).

	@:packed needs haxe 4.3+ — older compilers silently IGNORE the meta, leaving
	`pos` a plain null field, so the constructor's `this.pos.x` null-crashed the
	whole fixture (killing every demo after Rich.demo in Main's chain). Below 4.3
	the field is declared plain and allocated in the constructor instead: the
	debuggee survives, `pos` still renders as a Vec2 with x/y, and only the
	packed (inline-storage) layout itself goes untested on those versions.
**/
class PackedHolder {
	public var id:Int;
	#if (haxe_ver >= 4.3)
	@:packed public var pos(default, null):Vec2;
	#else
	public var pos(default, null):Vec2;
	#end
	public var tail:Int;

	public function new(id:Int, x:Float, y:Float) {
		this.id = id;
		#if (haxe_ver < 4.3)
		this.pos = new Vec2(0, 0); // packed storage is implicit on 4.3+; the plain field needs an instance
		#end
		this.pos.x = x;
		this.pos.y = y;
		this.tail = id * 2;
	}
}
