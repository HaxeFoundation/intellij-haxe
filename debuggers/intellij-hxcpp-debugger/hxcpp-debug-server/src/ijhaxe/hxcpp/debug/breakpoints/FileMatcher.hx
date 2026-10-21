package ijhaxe.hxcpp.debug.breakpoints;

/**
	Maps a source path from the IDE onto the runtime's own file key, the short
	name that `addFileLineBreakpoint` matches against. Matching is by path
	SUFFIX: the runtime file whose full path shares the most trailing path
	segments with the IDE path wins. A project built elsewhere (CI, a moved
	checkout, another drive) therefore still resolves, unlike with the
	runtime's own exact full-path comparison.
**/
class FileMatcher {
	final fullPaths:Array<String>;
	final fileKeys:Array<String>;

	/**
		`fullPaths` and `fileKeys` are index-aligned (Debugger.getFilesFullPath()
		and getFiles()).
	**/
	public function new(fullPaths:Array<String>, fileKeys:Array<String>) {
		this.fullPaths = fullPaths;
		this.fileKeys = fileKeys;
	}

	/**
		The runtime file key best matching `requestedPath`, or null when nothing
		shares even the final segment. The best match is the one sharing the most
		trailing segments; ties resolve to the first (registration order).
	**/
	public function resolve(requestedPath:String):Null<String> {
		var wanted = segments(requestedPath);
		if (wanted.length == 0) {
			return null;
		}
		var bestIndex = -1;
		var bestScore = 0;
		for (i in 0...fullPaths.length) {
			var score = sharedSuffixLength(wanted, segments(fullPaths[i]));
			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}
		return bestIndex >= 0 ? fileKeys[bestIndex] : null;
	}

	// The number of trailing path segments both paths share, compared
	// case-insensitively: on Windows the IDE and the runtime can disagree on
	// the casing of the drive letter or the path.
	static function sharedSuffixLength(a:Array<String>, b:Array<String>):Int {
		var i = a.length - 1;
		var j = b.length - 1;
		var shared = 0;
		while (i >= 0 && j >= 0 && a[i].toLowerCase() == b[j].toLowerCase()) {
			shared++;
			i--;
			j--;
		}
		return shared;
	}

	// Splits on both separators and drops empty parts, so "a//b" and a
	// trailing slash add no empty segments.
	static function segments(path:String):Array<String> {
		if (path == null) {
			return [];
		}
		var normalized = StringTools.replace(path, "\\", "/");
		return [for (part in normalized.split("/")) if (part != "") part];
	}
}
