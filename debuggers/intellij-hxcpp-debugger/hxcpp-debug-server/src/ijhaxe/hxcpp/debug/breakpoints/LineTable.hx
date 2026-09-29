package ijhaxe.hxcpp.debug.breakpoints;

/**
	The executable-line table: for each compiled file, the source lines that
	carry code the runtime instruments. hxcpp keeps no line table that can be
	queried at runtime; the generated `HXLINE(n)` markers are executed
	assignments, not data. `Macro.bakeLineTable` therefore records the table
	at compile time and stores it in the binary as a `haxe.Resource`.

	Breakpoints uses it to verify requested lines. A line without code is
	rejected (unverified). A stale binary, where code was edited or
	uncommented without a recompile, thus shows a hollow marker instead of a
	breakpoint that never fires.

	Format, one file per row: `<path as the compiler recorded it>|l1,l2,...`,
	with the lines sorted ascending. A binary built with an older version of
	this library has no such resource; it gets no table, and Breakpoints then
	checks only the file.
**/
class LineTable {
	/** The resource name; Macro.LINE_TABLE_RESOURCE must spell it the same. **/
	public static inline var RESOURCE_NAME = "ijhaxe.hxcpp.debug.lineTable";

	// table file path -> sorted executable lines
	final linesByFile:Map<String, Array<Int>>;
	final matcher:FileMatcher;

	function new(linesByFile:Map<String, Array<Int>>) {
		this.linesByFile = linesByFile;
		var files = [for (file in linesByFile.keys()) file];
		matcher = new FileMatcher(files, files);
	}

	/** The table compiled into this binary, or null when it has none. **/
	public static function fromResource():Null<LineTable> {
		var text = haxe.Resource.getString(RESOURCE_NAME);
		return text == null ? null : parse(text);
	}

	public static function parse(text:String):LineTable {
		var linesByFile = new Map<String, Array<Int>>();
		for (row in text.split("\n")) {
			var sep = row.lastIndexOf("|");
			if (sep <= 0) {
				continue;
			}
			var lines:Array<Int> = [];
			for (part in row.substr(sep + 1).split(",")) {
				var line = Std.parseInt(part);
				if (line != null) {
					lines.push(line);
				}
			}
			if (lines.length > 0) {
				linesByFile.set(row.substr(0, sep), lines);
			}
		}
		return new LineTable(linesByFile);
	}

	/**
		The executable lines of the table file that best matches `sourcePath`,
		using the same suffix matching as for the runtime file keys. Null when
		the table knows no such file; the caller then checks only the file.
	**/
	public function linesFor(sourcePath:String):Null<Array<Int>> {
		var file = matcher.resolve(sourcePath);
		return file == null ? null : linesByFile.get(file);
	}

	/** True when sorted `lines` contains `line` (binary search). **/
	public static function hasLine(lines:Array<Int>, line:Int):Bool {
		var lo = 0;
		var hi = lines.length - 1;
		while (lo <= hi) {
			var mid = (lo + hi) >> 1;
			var value = lines[mid];
			if (value == line) {
				return true;
			}
			if (value < line) {
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return false;
	}
}
