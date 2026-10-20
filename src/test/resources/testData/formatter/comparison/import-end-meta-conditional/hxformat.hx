package;

import haxe.ds.StringMap;
#if native
import haxe.io.Bytes;
import haxe.Timer;
#end

#if !my_debug
@:noDebug
#end
@:access(my.pack.Sample)
class Main {
	static function main() {
		trace(new StringMap<Int>());
	}
}
