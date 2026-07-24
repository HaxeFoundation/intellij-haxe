package ijhaxe.hxcpp.debug.breakpoints;

/**
	The compile-time-baked executable-line table: which source lines of each
	compiled file carry code the runtime instruments. Baked by
	`Macro.bakeLineTable` (a `Context.onGenerate` walk over the typed AST) into
	a `haxe.Resource`, because hxcpp keeps no queryable line table at runtime —
	the generated `HXLINE(n)` markers are executed assignments, not data.

	Breakpoints uses it to verify requested lines: a line with no code is
	rejected (unverified) so a stale binary — code edited or commented back in
	without a recompile — surfaces as a hollow marker instead of a breakpoint
	that never fires.

	Format, one file per row: `<path as the compiler recorded it>|l1,l2,...`
	with lines sorted ascending. A missing resource (older lib build) yields no
	table and verification degrades to file-level.
**/
class LineTable {
	/** Resource key; Macro.hx bakes under the same name (kept in sync there). **/
	public static inline var RESOURCE_NAME = "ijhaxe.hxcpp.debug.lineTable";

	// table file path -> sorted executable lines
	final linesByFile:Map<String, Array<Int>>;
	final matcher:FileMatcher;

	function new(linesByFile:Map<String, Array<Int>>) {
		this.linesByFile = linesByFile;
		var files = [for (file in linesByFile.keys()) file];
		matcher = new FileMatcher(files, files);
	}

	/** The table baked into this binary, or null when none was baked. **/
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
		The executable lines of the table file best matching `sourcePath`
		(same suffix matching as the runtime file keys), or null when the
		table knows no such file — the caller degrades to file-level then.
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
