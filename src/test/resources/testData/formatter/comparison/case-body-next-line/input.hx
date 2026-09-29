class Main {
	static function count(types:Array<Kind>):Int {
		var pieces = 0;
		for (type in types) {
			switch (type) {
				case BEGIN_FILL, BEGIN_GRADIENT_FILL: pieces++;
				case DRAW_QUADS, DRAW_TRIANGLES: pieces += 2;
				case OTHER:
					pieces += 3;
				default:
			}
		}
		var label = switch (pieces) {
			case 0: "empty";
			case 1: "single";
			default: "many";
		}
		return pieces + label.length;
	}
}
