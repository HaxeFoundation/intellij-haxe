package tests;

import ijhaxe.hxcpp.debug.breakpoints.FileMatcher;

class FileMatcherTest {
	public static function run(assert:Assert):Void {
		exactPathMatches(assert);
		movedProjectStillMatchesBySuffix(assert);
		theLongestSuffixWins(assert);
		mixedSeparatorsAndCaseAreTolerated(assert);
		noSharedSegmentReturnsNull(assert);
	}

	static function make():FileMatcher {
		return new FileMatcher(
			["C:/build/src/Main.hx", "C:/build/src/pkg/Util.hx", "C:/haxe/std/Sys.hx"],
			["Main.hx", "pkg/Util.hx", "Sys.hx"]);
	}

	static function exactPathMatches(assert:Assert):Void {
		assert.equals("Main.hx", make().resolve("C:/build/src/Main.hx"), "exact path resolves to its key");
	}

	static function movedProjectStillMatchesBySuffix(assert:Assert):Void {
		// built on CI under a different root; the IDE opens it from elsewhere
		assert.equals("pkg/Util.hx", make().resolve("D:/checkout/src/pkg/Util.hx"), "moved project matches by suffix");
	}

	static function theLongestSuffixWins(assert:Assert):Void {
		// two runtime files share the final segment; the deeper shared path wins
		var matcher = new FileMatcher(
			["C:/a/pkg/Main.hx", "C:/b/Main.hx"],
			["a-key", "b-key"]);
		assert.equals("a-key", matcher.resolve("X:/somewhere/pkg/Main.hx"), "the longer shared suffix wins");
	}

	static function mixedSeparatorsAndCaseAreTolerated(assert:Assert):Void {
		assert.equals("Main.hx", make().resolve("c:\\BUILD\\src\\main.hx"), "backslashes and casing tolerated");
	}

	static function noSharedSegmentReturnsNull(assert:Assert):Void {
		assert.isTrue(make().resolve("C:/other/Nope.hx") == null, "no shared final segment -> null");
	}
}
