/**
 * Pause fixture: a long busy loop of pure Haxe work, so a user pause interrupts
 * the program IN Haxe code (with real frames + locals) rather than inside a
 * native call. Bounded by wall-clock so it runs ~3s regardless of machine speed
 * and then exits; the hot inner loop is pure integer arithmetic so a pause almost
 * always lands there, not in the occasional native Sys.time check.
 */
class Spin {
	public static function main():Void {
		Sys.println("spin-start");
		var start = Sys.time();
		var total = 0;
		var i = 0;
		while (Sys.time() - start < 3.0) {
			for (j in 0...20000000) {
				total += (i & 7);
				i++;
			}
		}
		Sys.println("spin-done:" + total);
	}
}
