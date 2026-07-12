package pkg;

/**
 * A PACKAGED class with statics: evaluate must resolve `pkg.Deep.marker`
 * by matching the dotted class prefix (the root "pkg" alone is not a class).
 */
class Deep {
	public static var marker:Int = 99;

	public static function touch():Int {
		return marker;
	}
}
