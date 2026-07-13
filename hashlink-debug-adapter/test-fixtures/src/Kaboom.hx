/** A subclass of Boom, to exercise subtype matching in the type filter (M42). */
class Kaboom extends Boom {
	public function new(message:String) {
		super(message);
	}
}
