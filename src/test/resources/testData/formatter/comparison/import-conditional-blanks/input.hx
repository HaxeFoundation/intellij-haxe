package;

import haxe.ds.StringMap;


#if sys

import sys.io.File;

#else

import haxe.Timer;

#end

import haxe.io.Bytes;


using StringTools;


class Main {
	static function main() {
		var names = new StringMap<Int>();
		names.set("total", 1);
		trace(names.get("total"));
	}
}
