package limeparser;

/**
	Pure assertions over the evaluator, run with --interp (no runtime needed).
	Each case feeds inline XML through a fresh evaluator; a failed assertion
	prints the case and exits non-zero so the gradle task fails.
**/
class TestMain {
	static var failures = 0;

	static function main():Void {
		conditionalHaxelibFollowsDefines();
		unlessExcludesOnMatch();
		orAndConditionCombinations();
		haxelibNameBecomesDefine();
		setUnsetAndSubstitution();
		sectionAndDefineCollection();
		includeResolvesThroughResolver();
		sourcePathsCollected();
		haxelibResolutionMergesIncludeXml();
		haxelibPathOutputParsing();
		assetLibraryHandlerPullsHandlerHaxelib();

		if (failures > 0) {
			Sys.stderr().writeString(failures + " assertion(s) failed\n");
			Sys.exit(1);
		}
		Sys.println("LimeProjectParser tests passed");
	}

	static function evaluate(xml:String, ?seeds:Map<String, String>,
			?includes:String->Null<String>):ProjectXmlEvaluator {
		var evaluator = new ProjectXmlEvaluator(
			seeds != null ? seeds : new Map(),
			"display",
			new Map(),
			includes != null ? includes : ProjectXmlEvaluator.NO_INCLUDES);
		evaluator.parse(xml);
		return evaluator;
	}

	static function conditionalHaxelibFollowsDefines():Void {
		var xml = '<project>
			<haxelib name="openfl"/>
			<haxelib name="hlsteam" if="hl"/>
			<haxelib name="jsdeps" if="html5"/>
		</project>';

		var withHl = evaluate(xml, ["hl" => ""]);
		assertTrue(hasLib(withHl, "hlsteam"), "hl build includes hlsteam");
		assertTrue(!hasLib(withHl, "jsdeps"), "hl build excludes jsdeps");

		var withHtml5 = evaluate(xml, ["html5" => ""]);
		assertTrue(!hasLib(withHtml5, "hlsteam"), "html5 build excludes hlsteam");
		assertTrue(hasLib(withHtml5, "jsdeps"), "html5 build includes jsdeps");
	}

	static function unlessExcludesOnMatch():Void {
		var xml = '<project><haxelib name="polyfill" unless="hl"/></project>';
		assertTrue(!hasLib(evaluate(xml, ["hl" => ""]), "polyfill"), "unless match excludes");
		assertTrue(hasLib(evaluate(xml, ["html5" => ""]), "polyfill"), "unless mismatch keeps");
	}

	static function orAndConditionCombinations():Void {
		var xml = '<project><haxedef name="picked" if="html5 debug || hl"/></project>';
		assertTrue(evaluate(xml, ["hl" => ""]).haxedefs.exists("picked"), "OR segment alone matches");
		assertTrue(evaluate(xml, ["html5" => "", "debug" => ""]).haxedefs.exists("picked"), "AND segment matches");
		assertTrue(!evaluate(xml, ["html5" => ""]).haxedefs.exists("picked"), "partial AND segment fails");
	}

	static function haxelibNameBecomesDefine():Void {
		var xml = '<project>
			<haxelib name="openfl"/>
			<haxedef name="uses-openfl" if="openfl"/>
		</project>';
		assertTrue(evaluate(xml).haxedefs.exists("uses-openfl"), "haxelib name usable as condition");
	}

	static function setUnsetAndSubstitution():Void {
		// $$ = literal $ in a single-quoted haxe string (interpolation escape)
		var xml = '<project>
			<set name="flavor" value="pro"/>
			<haxedef name="edition" value="edition-$${flavor}"/>
			<unset name="flavor"/>
			<haxedef name="late" if="flavor"/>
		</project>';
		var result = evaluate(xml);
		assertEquals("edition-pro", result.haxedefs.get("edition"), "variable substitution");
		assertTrue(!result.haxedefs.exists("late"), "unset removes the define");
	}

	static function sectionAndDefineCollection():Void {
		var xml = '<project>
			<section if="hl">
				<define name="native"/>
			</section>
		</project>';
		var result = evaluate(xml, ["hl" => ""]);
		assertTrue(result.haxedefs.exists("native"), "section content applies");
		assertTrue(result.defines.exists("native"), "define lands in defines too");
		assertTrue(!evaluate(xml).haxedefs.exists("native"), "unmatched section skipped");
	}

	static function includeResolvesThroughResolver():Void {
		var xml = '<project><include path="common.xml"/></project>';
		var result = evaluate(xml, null,
			path -> path == "common.xml" ? '<project><haxelib name="shared"/></project>' : null);
		assertTrue(hasLib(result, "shared"), "included file's entries collected");
	}

	static function sourcePathsCollected():Void {
		var xml = '<project>
			<source path="src"/>
			<classpath path="vendor/lib/src" if="hl"/>
		</project>';
		var result = evaluate(xml, ["hl" => ""]);
		assertTrue(result.sources.contains("src"), "source collected");
		assertTrue(result.sources.contains("vendor/lib/src"), "conditional classpath collected");
	}

	static function haxelibResolutionMergesIncludeXml():Void {
		// fake `haxelib path openfl`: openfl + its transitive dep swf; openfl
		// ships an include.xml adding a haxedef and another haxelib (lime)
		var resolver = (name:String, version:String) -> {
			return switch (name) {
				case "openfl": [
						{name: "swf", version: "3.4.0", root: "/lib/swf/3,4,0",
							classpaths: ["/lib/swf/3,4,0/src"], includeXml: null,
							extraDefines: [], extraArgs: []},
						{name: "openfl", version: "9.5.0", root: "/lib/openfl/9,5,0",
							classpaths: ["/lib/openfl/9,5,0/src"],
							includeXml: '<project>
								<haxedef name="openfl-shipped"/>
								<haxelib name="lime"/>
								<source path="extra"/>
							</project>',
							extraDefines: ["from-extra-params=on"], extraArgs: ["--macro Something.include()"]}
					];
				case "lime": [
						{name: "lime", version: "8.3.2", root: "/lib/lime/8,3,2",
							classpaths: ["/lib/lime/8,3,2/src"], includeXml: null,
							extraDefines: [], extraArgs: []}
					];
				default: null;
			}
		};
		var evaluator = new ProjectXmlEvaluator(new Map(), "display", new Map(),
			ProjectXmlEvaluator.NO_INCLUDES, resolver);
		evaluator.parse('<project><haxelib name="openfl"/></project>');

		assertTrue(hasLib(evaluator, "openfl"), "declared lib listed");
		assertTrue(hasLib(evaluator, "swf"), "haxelib.json transitive dep listed");
		assertTrue(hasLib(evaluator, "lime"), "include.xml haxelib listed");
		assertEquals("9.5.0", evaluator.defines.get("openfl"), "version define from resolution");
		// the resolved version is NOT a pin: echoing it would later request that
		// release over the repository's current (git/dev) selection
		assertEquals("", libVersion(evaluator, "openfl"), "unpinned declaration stays unpinned");
		assertEquals("", libVersion(evaluator, "swf"), "transitive libraries carry no pin");
		assertTrue(evaluator.haxedefs.exists("openfl-shipped"), "include.xml haxedef merged");
		assertTrue(evaluator.sources.contains("/lib/swf/3,4,0/src"), "transitive classpath collected");
		var rebased = Lambda.exists(evaluator.sources, source -> StringTools.endsWith(source, "extra")
			&& StringTools.startsWith(source, "/lib/openfl"));
		assertTrue(rebased, "include.xml source rebased to the library root");
		assertEquals("on", evaluator.haxedefs.get("from-extra-params"), "extraParams define merged");
	}

	static function haxelibPathOutputParsing():Void {
		// the exact per-library line order haxelib prints: -L ndll, extraParams
		// content inline, classpaths, then the -D name=version marker
		var libraries = HaxelibLookup.parseOutput('-L /lib/lime/8,3,2/ndll/
--macro lime._internal.macros.DefineMacro.run()
-D lime-extra=1
/lib/lime/8,3,2/src/
-D lime=8.3.2
/lib/actuate/1,9,0/src/
-D actuate=1.9.0
');
		assertEquals("2", Std.string(libraries.length), "two libraries parsed");
		assertEquals("lime", libraries[0].name, "first library name");
		assertEquals("8.3.2", libraries[0].version, "first library version");
		assertTrue(libraries[0].classpaths.contains("/lib/lime/8,3,2/src"), "classpath without trailing slash");
		assertTrue(libraries[0].extraDefines.contains("lime-extra=1"), "extraParams -D kept off the marker path");
		assertTrue(libraries[0].extraArgs.length == 2, "macro and -L lines collected as extra args");
		assertEquals("actuate", libraries[1].name, "second library name");
	}

	static function assetLibraryHandlerPullsHandlerHaxelib():Void {
		// the openfl scenario: its include.xml registers the swf handler; a project
		// swf asset library then pulls the swf haxelib in (handler registration
		// arrives AFTER the asset declaration - resolution runs at end of parse)
		var resolver = (name:String, version:String) -> {
			return switch (name) {
				case "openfl": [
						{name: "openfl", version: "9.5.0", root: "/lib/openfl/9,5,0",
							classpaths: ["/lib/openfl/9,5,0/src"],
							includeXml: '<project><library handler="swf" type="swf"/></project>',
							extraDefines: [], extraArgs: []}
					];
				case "swf": [
						{name: "swf", version: "3.4.0", root: "/lib/swf/3,4,0",
							classpaths: ["/lib/swf/3,4,0/src"], includeXml: null,
							extraDefines: [], extraArgs: []}
					];
				default: null;
			}
		};
		var evaluator = new ProjectXmlEvaluator(new Map(), "display", new Map(),
			ProjectXmlEvaluator.NO_INCLUDES, resolver);
		evaluator.parse('<project>
			<library path="assets/graphics.swf"/>
			<haxelib name="openfl"/>
		</project>');

		assertTrue(hasLib(evaluator, "swf"), "swf handler haxelib pulled by the asset library");
		assertTrue(evaluator.sources.contains("/lib/swf/3,4,0/src"), "handler classpath collected");

		var withoutAssets = new ProjectXmlEvaluator(new Map(), "display", new Map(),
			ProjectXmlEvaluator.NO_INCLUDES, resolver);
		withoutAssets.parse('<project><haxelib name="openfl"/></project>');
		assertTrue(!hasLib(withoutAssets, "swf"), "no swf assets - no swf haxelib");
	}

	static function hasLib(evaluator:ProjectXmlEvaluator, name:String):Bool {
		return Lambda.exists(evaluator.haxelibs, lib -> lib.name == name);
	}

	static function libVersion(evaluator:ProjectXmlEvaluator, name:String):Null<String> {
		var found = Lambda.find(evaluator.haxelibs, lib -> lib.name == name);
		return found == null ? null : found.version;
	}

	static function assertTrue(condition:Bool, label:String):Void {
		if (!condition) {
			Sys.stderr().writeString("FAILED: " + label + "\n");
			failures++;
		}
	}

	static function assertEquals(expected:String, actual:String, label:String):Void {
		if (expected != actual) {
			Sys.stderr().writeString("FAILED: " + label + " - expected '" + expected + "' got '" + actual + "'\n");
			failures++;
		}
	}
}
