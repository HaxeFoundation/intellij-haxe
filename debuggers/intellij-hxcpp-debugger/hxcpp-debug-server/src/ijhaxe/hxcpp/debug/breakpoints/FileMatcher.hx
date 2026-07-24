package ijhaxe.hxcpp.debug.breakpoints;

/**
	Maps an IDE-supplied source path onto the runtime's own file key (the short
	name `addFileLineBreakpoint` matches against). Matching is by path SUFFIX —
	the longest run of trailing path segments shared between the IDE path and a
	runtime full path — so a project built elsewhere (CI, a moved checkout,
	different drive) still resolves, unlike the runtime's own exact-full-path
	comparison. Pure; unit-tested.
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
			var score = sharedSuffix(wanted, segments(fullPaths[i]));
			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}
		return bestIndex >= 0 ? fileKeys[bestIndex] : null;
	}

	// Trailing path segments common to both, compared case-insensitively (the
	// IDE and the runtime can disagree on drive-letter / path casing on Windows).
	static function sharedSuffix(a:Array<String>, b:Array<String>):Int {
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

	// Split on both separators; drop empties so "a//b" and a trailing slash do
	// not create phantom segments.
	static function segments(path:String):Array<String> {
		if (path == null) {
			return [];
		}
		var normalized = StringTools.replace(path, "\\", "/");
		return [for (part in normalized.split("/")) if (part != "") part];
	}
}
