/**
	A subclass of Boom, to exercise subtype matching in the type filter.
**/
class Kaboom extends Boom {
	public function new(message:String) {
		super(message);
	}
}
