package pkg;

/**
	A PACKAGED class with statics: evaluate must resolve `pkg.Deep.marker`
	by matching the dotted class prefix (the root "pkg" alone is not a class).
**/
class Deep {
	public static var marker:Int = 99;

	public static function touch():Int {
		return new Deep().readMarker();
	}

	public static final CONSTANT:Int = 42;

	public function new() {}

	// An INSTANCE frame inside a PACKAGED class: evaluate must still find the
	// class's own statics (marker/CONSTANT) without a class prefix.
	public function readMarker():Int {
		return marker + CONSTANT; // FIXTURE_DEEP_INSTANCE_LINE = 21
	}
}
