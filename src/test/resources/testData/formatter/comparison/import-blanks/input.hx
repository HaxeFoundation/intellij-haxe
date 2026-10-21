package;

import haxe.ds.StringMap;

import haxe.io.Bytes;


import sys.io.File;
using StringTools;

class Main {
	static function main() {
		var m = new StringMap<Int>();
		m.set("k", 1);
		trace(m);
	}
}
