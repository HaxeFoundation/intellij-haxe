package limeparser;

import haxe.io.Path;
import sys.FileSystem;

/** Scratch directories for the file-system cases; a test deletes what it created. **/
class TempFiles {
	public static function createDirectory():String {
		var base = Sys.getEnv("TEMP");
		if (base == null) base = Sys.getEnv("TMPDIR");
		if (base == null) base = "/tmp";
		var directory = Path.join([base, "limeparser-test-" + Std.string(Std.random(0x7FFFFFFF))]);
		FileSystem.createDirectory(directory);
		return directory;
	}

	public static function delete(directory:String):Void {
		for (entry in FileSystem.readDirectory(directory)) {
			var path = Path.join([directory, entry]);
			if (FileSystem.isDirectory(path)) {
				delete(path);
			} else {
				FileSystem.deleteFile(path);
			}
		}
		FileSystem.deleteDirectory(directory);
	}
}
