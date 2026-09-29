package;

import haxe.ds.StringMap;
import haxe.io.Bytes;

import sys.io.File;

import sys.FileSystem;
import a.b.Widget;
import Std;
using StringTools;

class Main {
	static function main() {
		var m = new StringMap<Int>();
		trace(m + Std.string(Bytes.alloc(1)) + File + FileSystem + Widget);
	}
}
