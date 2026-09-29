package;
import haxe.ds.StringMap;
import haxe.io.Path;
using StringTools;
class Main {
	static function main() {
		var map = new StringMap<Int>();
		map.set(Path.join(["a", "b"]).trim(), 1);



		trace(map);
	}
}
