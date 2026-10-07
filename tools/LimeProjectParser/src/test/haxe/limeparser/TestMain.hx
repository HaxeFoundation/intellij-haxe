package limeparser;

import haxe.io.Path;
import limeparser.ProjectXmlEvaluator.IncludeFile;
import limeparser.ProjectXmlEvaluator.ResolvedHaxelib;
import limeparser.ProjectXmlEvaluator.Resolvers;
import sys.FileSystem;
import sys.io.File;

/**
	Pure assertions over the evaluator, run with --interp (no runtime needed).
	Each case feeds inline XML through a fresh evaluator; a failed assertion
	prints the case and exits non-zero so the gradle task fails. The
	file-system cases write into a scratch directory they delete again.
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
		haxedefRemoveDropsTheHaxedef();
		haxeflagDefineFoldsIntoHaxedefs();
		setBuildDirMovesTheExportRoot();
		unknownVariableStaysLiteral();
		comparisonsResolveAsStrings();
		haxelibVariableIsTheLibraryRoot();
		projectDirectoryVariable();
		includeDirectoryPrefersLimeThenNmmlThenXml();
		includeFolderIsPathBaseAndSource();
		includeHaxelibMergesWithoutRegistering();
		localHaxelibPathReadsHaxelibJson();
		localHaxelibPathDeclaresWithoutLookup();
		optionalMissingHaxelibIsSkipped();
		libraryRootWalksUpToHaxelibJson();
		webTargetSeeds();
		desktopTargetSeeds();
		pseudoTargetSeedsUseTheHost();
		mobileAndDebugSeeds();
		toolsDefineFollowsTheLimeHaxelib();
		haxeVersionSeeds();
		targetNamedEnvironmentVariablesAreDropped();
		configValuesFlattenedToDotKeys();

		if (failures > 0) {
			Sys.stderr().writeString(failures + " assertion(s) failed\n");
			Sys.exit(1);
		}
		Sys.println("LimeProjectParser tests passed");
	}

	static function evaluate(xml:String, ?seeds:Map<String, String>, ?resolvers:Resolvers,
			?environment:Map<String, String>):ProjectXmlEvaluator {
		var evaluator = new ProjectXmlEvaluator(
			seeds != null ? seeds : new Map(),
			"display",
			environment != null ? environment : new Map(),
			"",
			resolvers != null ? resolvers : ProjectXmlEvaluator.resolversOf());
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
			<setenv name="FLAG"/>
		</project>';
		var result = evaluate(xml);
		assertEquals("edition-pro", result.haxedefs.get("edition"), "variable substitution");
		assertTrue(!result.haxedefs.exists("late"), "unset removes the define");
		assertEquals("1", result.defines.get("FLAG"), "setenv without a value sets 1");
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
		var resolvers = ProjectXmlEvaluator.resolversOf(
			path -> path == "common.xml" ? {path: path, content: '<project><haxelib name="shared"/></project>'} : null);
		var result = evaluate(xml, null, resolvers);
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
		var openflInclude = '<project>
			<haxedef name="openfl-shipped"/>
			<haxelib name="lime"/>
			<source path="extra"/>
		</project>';
		var resolver = (name:String, version:String) -> {
			return switch (name) {
				case "openfl": [
						library("swf", "3.4.0", "/lib/swf/3,4,0"),
						library("openfl", "9.5.0", "/lib/openfl/9,5,0", openflInclude, ["from-extra-params=on"],
							["--macro Something.include()"])
					];
				case "lime": [library("lime", "8.3.2", "/lib/lime/8,3,2")];
				default: null;
			}
		};
		var evaluator = evaluate('<project><haxelib name="openfl"/></project>', null,
			ProjectXmlEvaluator.resolversOf(null, resolver));

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
		assertTrue(evaluator.sources.contains("/lib/openfl/9,5,0/extra"), "include.xml source rebased to the library root");
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
				case "openfl": [library("openfl", "9.5.0", "/lib/openfl/9,5,0", '<project><library handler="swf" type="swf"/></project>')];
				case "swf": [library("swf", "3.4.0", "/lib/swf/3,4,0")];
				default: null;
			}
		};
		var resolvers = ProjectXmlEvaluator.resolversOf(null, resolver);
		var evaluator = evaluate('<project>
			<library path="assets/graphics.swf"/>
			<haxelib name="openfl"/>
		</project>', null, resolvers);

		assertTrue(hasLib(evaluator, "swf"), "swf handler haxelib pulled by the asset library");
		assertTrue(evaluator.sources.contains("/lib/swf/3,4,0/src"), "handler classpath collected");

		var withoutAssets = evaluate('<project><haxelib name="openfl"/></project>', null, resolvers);
		assertTrue(!hasLib(withoutAssets, "swf"), "no swf assets - no swf haxelib");
	}

	static function haxedefRemoveDropsTheHaxedef():Void {
		var xml = '<project>
			<define name="kept"/>
			<haxedef name="gone"/>
			<haxedef remove="gone"/>
			<haxedef remove="kept"/>
		</project>';
		var result = evaluate(xml);
		assertTrue(!result.haxedefs.exists("gone"), "remove drops the haxedef");
		assertTrue(!result.haxedefs.exists("kept"), "remove drops a <define>'s haxedef");
		assertTrue(result.defines.exists("kept"), "remove keeps the condition define");
	}

	static function haxeflagDefineFoldsIntoHaxedefs():Void {
		var xml = '<project>
			<haxeflag name="-D" value="split=1"/>
			<haxeflag name="-D joined"/>
			<compilerflag name="-D" value="compiler"/>
			<haxeflag name="--remap" value="flash:openfl"/>
		</project>';
		var result = evaluate(xml);
		assertEquals("1", result.haxedefs.get("split"), "-D as name, name=value as value");
		assertTrue(result.haxedefs.exists("joined"), "-D name in one attribute");
		assertTrue(result.haxedefs.exists("compiler"), "<compilerflag> is the same element");
		assertTrue(!result.haxedefs.exists("--remap"), "a flag other than -D is no haxedef");
	}

	static function setBuildDirMovesTheExportRoot():Void {
		var result = evaluate('<project><app path="Export"/><set name="BUILD_DIR" value="out"/></project>');
		assertEquals("out", result.appPath, "BUILD_DIR replaces the app path");
		assertEquals("out", result.defines.get("BUILD_DIR"), "and stays a define");
	}

	static function unknownVariableStaysLiteral():Void {
		var xml = '<project>
			<haxedef name="value" value="$${nope}"/>
			<haxedef name="guarded" if="$${nope}"/>
		</project>';
		var result = evaluate(xml);
		assertEquals("nope", result.haxedefs.get("value"), "an unknown name passes through unchanged");
		assertTrue(!result.haxedefs.exists("guarded"), "an unknown name is no define, so the condition fails");
	}

	static function comparisonsResolveAsStrings():Void {
		// lime's include.xml spells a version check as ${${haxe_ver} < 4}: the
		// inner reference stays literal in round one, the comparison resolves in round two
		var xml = '<project>
			<haxedef name="old" if="$${$${haxe_ver} < 4}"/>
			<haxedef name="new" if="$${$${haxe_ver} >= 4.3}"/>
			<haxedef name="exact" if="$${haxe_ver == 4.3.7}"/>
			<haxedef name="other" if="$${haxe_ver != 4.3.7}"/>
			<haxedef name="spaced" value="$${ haxe_ver > 4.3.7 }"/>
		</project>';
		var result = evaluate(xml, null, null, ["haxe_ver" => "4.3.7"]);
		assertTrue(!result.haxedefs.exists("old"), "nested comparison false");
		assertTrue(result.haxedefs.exists("new"), "nested comparison true");
		assertTrue(result.haxedefs.exists("exact"), "== against the environment");
		assertTrue(!result.haxedefs.exists("other"), "!= false");
		assertEquals("false", result.haxedefs.get("spaced"), "whitespace is stripped; the result is a string");
	}

	static function haxelibVariableIsTheLibraryRoot():Void {
		var resolvers = ProjectXmlEvaluator.resolversOf(null,
			(name, version) -> name == "openfl" ? [library("openfl", "9.5.0", "C:\\lib\\openfl/9,5,0/")] : null);
		var xml = '<project>
			<set name="root" value="$${haxelib:openfl}"/>
			<set name="missing" value="$${haxelib:ghost}"/>
		</project>';
		var result = evaluate(xml, null, resolvers);
		assertEquals("C:/lib/openfl/9,5,0", result.defines.get("root"), "root with forward slashes, no trailing slash");
		assertEquals("", result.defines.get("missing"), "an unknown library resolves to empty");
	}

	static function projectDirectoryVariable():Void {
		var evaluator = new ProjectXmlEvaluator(new Map(), "display", new Map(), "/work/game", ProjectXmlEvaluator.resolversOf());
		evaluator.parse('<project><set name="dir" value="$${projectDirectory}"/></project>');
		assertEquals("/work/game", evaluator.defines.get("dir"), "projectDirectory is the project folder");
	}

	static function includeDirectoryPrefersLimeThenNmmlThenXml():Void {
		var directory = TempFiles.createDirectory();
		var included = Path.join([directory, "ext"]);
		FileSystem.createDirectory(included);
		File.saveContent(Path.join([included, "include.xml"]), '<project><haxedef name="xml"/></project>');
		assertEquals("ext/include.xml", IncludeFiles.resolve(directory, "ext").path, "a directory stands for its include.xml");

		File.saveContent(Path.join([included, "include.nmml"]), '<project><haxedef name="nmml"/></project>');
		assertEquals("ext/include.nmml", IncludeFiles.resolve(directory, "ext").path, "include.nmml wins over include.xml");

		File.saveContent(Path.join([included, "include.lime"]), '<project><haxedef name="lime"/></project>');
		var resolved = IncludeFiles.resolve(directory, "ext");
		assertEquals("ext/include.lime", resolved.path, "include.lime wins over both");
		assertTrue(resolved.content.indexOf('name="lime"') >= 0, "content read from the chosen file");
		assertTrue(IncludeFiles.resolve(directory, "nowhere") == null, "a missing reference resolves to null");

		TempFiles.delete(directory);
	}

	static function includeFolderIsPathBaseAndSource():Void {
		var xml = '<project><include path="defs.xml"/><include path="ext"/></project>';
		var result = evaluate(xml, null, ProjectXmlEvaluator.resolversOf(includeFixture));
		assertEquals("gen,ext,ext/src", result.sources.join(","), "the folder precedes the include's own sources; a top-level file adds none");
		assertTrue(result.haxedefs.exists("nested"), "a nested include resolves against the including file's folder");
	}

	/** A top-level file, a directory standing for its include.xml, and a file that one includes relatively. **/
	static function includeFixture(reference:String):Null<IncludeFile> {
		return switch (reference) {
			case "defs.xml": {path: "defs.xml", content: '<project><source path="gen"/></project>'};
			case "ext": {path: "ext/include.xml", content: '<project><source path="src"/><include path="more.xml"/></project>'};
			case "ext/more.xml": {path: "ext/more.xml", content: '<project><haxedef name="nested"/></project>'};
			default: null;
		}
	}

	static function includeHaxelibMergesWithoutRegistering():Void {
		var ext = library("ext", "1.0.0", "/lib/ext/1,0,0", '<project><haxedef name="from-ext"/><source path="extra"/></project>');
		var resolvers = ProjectXmlEvaluator.resolversOf(null, (name, version) -> name == "ext" ? [ext] : null);
		var result = evaluate('<project><include haxelib="ext"/></project>', null, resolvers);
		assertTrue(result.haxedefs.exists("from-ext"), "the library's include file is merged");
		assertTrue(result.sources.contains("/lib/ext/1,0,0/extra"), "its paths resolve against the library root");
		assertTrue(!hasLib(result, "ext"), "the library itself is not registered");
		assertTrue(!result.sources.contains("/lib/ext/1,0,0/src"), "nor its classpath");
	}

	static function localHaxelibPathReadsHaxelibJson():Void {
		var directory = TempFiles.createDirectory();
		var root = Path.join([directory, "vendor/mylib"]);
		FileSystem.createDirectory(root);
		File.saveContent(Path.join([root, "haxelib.json"]), '{"name": "mylib", "version": "1.2.0", "classPath": "src"}');
		File.saveContent(Path.join([root, "include.xml"]), '<project><haxedef name="from-mylib"/></project>');

		var local = HaxelibLookup.local(directory, "mylib", "vendor/mylib");
		assertEquals("1.2.0", local.version, "version from haxelib.json");
		assertEquals(Path.join([root, "src"]), local.classpaths[0], "classpath from haxelib.json's classPath");
		assertTrue(local.includeXml != null, "include file read from the root");
		assertTrue(HaxelibLookup.local(directory, "ghost", "vendor/ghost") == null, "a missing folder resolves to null");

		TempFiles.delete(directory);
	}

	static function localHaxelibPathDeclaresWithoutLookup():Void {
		// haxelib would answer with another version; the path declaration must win without asking
		var resolvers = ProjectXmlEvaluator.resolversOf(null,
			(name, version) -> [library(name, "9.9.9", "/haxelib/" + name)],
			(name, root) -> library(name, "1.2.0", root, '<project><haxedef name="from-local"/></project>'));
		var xml = '<project>
			<haxelib name="mylib" path="vendor/mylib"/>
			<haxelib name="pinned" version="2.0.0" path="vendor/pinned"/>
			<set name="root" value="$${haxelib:mylib}"/>
		</project>';
		var result = evaluate(xml, null, resolvers);
		assertEquals("1.2.0", result.defines.get("mylib"), "defined with the checkout's version, not haxelib's");
		assertEquals("", libVersion(result, "mylib"), "listed unpinned");
		assertEquals("2.0.0", libVersion(result, "pinned"), "a declared version stays the pin");
		assertTrue(result.sources.contains("vendor/mylib/src"), "classpath collected");
		assertTrue(!result.sources.contains("/haxelib/mylib/src"), "haxelib's classpath never consulted");
		assertTrue(result.haxedefs.exists("from-local"), "include file merged");
		assertEquals("vendor/mylib", result.defines.get("root"), "haxelib: variable reads the local root");
	}

	static function optionalMissingHaxelibIsSkipped():Void {
		var xml = '<project>
			<haxelib name="ghost" optional="true"/>
			<haxelib name="required"/>
			<haxedef name="uses-ghost" if="ghost"/>
		</project>';
		var result = evaluate(xml);
		assertTrue(!hasLib(result, "ghost"), "an optional library that does not resolve is skipped");
		assertTrue(!result.defines.exists("ghost"), "and defines nothing");
		assertTrue(!result.haxedefs.exists("uses-ghost"), "so a condition on it fails");
		assertTrue(hasLib(result, "required"), "a required one still counts as declared");
	}

	static function libraryRootWalksUpToHaxelibJson():Void {
		var directory = TempFiles.createDirectory();
		var root = Path.join([directory, "lib/1,0,0"]);
		FileSystem.createDirectory(Path.join([root, "src/pkg"]));
		File.saveContent(Path.join([root, "haxelib.json"]), '{"name": "lib", "version": "1.0.0"}');
		File.saveContent(Path.join([root, "include.xml"]), '<project><haxedef name="from-lib"/></project>');

		assertEquals(root, HaxelibLookup.libraryRoot(Path.join([root, "src/pkg"])), "the first folder above the classpath with a haxelib.json");
		var libraries = HaxelibLookup.parseOutput(Path.join([root, "src"]) + "/\n-D lib=1.0.0\n");
		assertEquals(root, libraries[0].root, "parsed library root");
		assertTrue(libraries[0].includeXml != null, "include.xml read from the root");
		assertEquals(directory, HaxelibLookup.libraryRoot(directory), "no haxelib.json above: the classpath itself");

		TempFiles.delete(directory);
	}

	static function webTargetSeeds():Void {
		var html5 = TargetDefines.resolve("html5", "windows", false);
		assertEquals("html5,web", keys(html5.haxedefs), "html5 -D set");
		assertEquals("1", html5.conditions.get("html5"), "target define");
		assertEquals("web", html5.conditions.get("platformType"), "platform type");
		// lime sets targetType=js only for firefox and uwp builds, not for plain html5
		assertTrue(!html5.conditions.exists("targetType"), "no target type for html5");
		assertEquals("release", html5.conditions.get("buildType"), "release without -debug");
		assertTrue(!html5.conditions.exists("native"), "html5 is not native");

		var flash = TargetDefines.resolve("flash", "windows", false);
		assertEquals("web", keys(flash.haxedefs), "flash gets no -D flash, only its platform type");
		assertEquals("swf", flash.conditions.get("targetType"), "flash target type");
		assertEquals("1", flash.conditions.get("flash"), "flash condition define");
	}

	static function desktopTargetSeeds():Void {
		var windows = TargetDefines.resolve("windows", "windows", false);
		assertEquals("desktop,windows", keys(windows.haxedefs), "windows -D set");
		assertEquals("cpp", windows.conditions.get("targetType"), "a host desktop build is cpp");
		assertEquals("1", windows.conditions.get("native"), "and native");

		// a desktop platform other than the host is cross-compiled as a neko build
		var mac = TargetDefines.resolve("mac", "windows", false);
		assertEquals("desktop,mac,macos", keys(mac.haxedefs), "mac adds -D macos");
		assertEquals("neko", mac.conditions.get("targetType"), "mac from a windows host is neko");
		assertTrue(!mac.conditions.exists("cpp"), "no cpp define");

		var air = TargetDefines.resolve("air", "windows", false);
		assertEquals("air,desktop", keys(air.haxedefs), "air -D set");
		assertEquals("swf", air.conditions.get("targetType"), "air target type");
		assertEquals("1", air.conditions.get("flash"), "air defines flash");
	}

	static function pseudoTargetSeedsUseTheHost():Void {
		var hl = TargetDefines.resolve("hl", "windows", false);
		assertEquals("desktop,windows", keys(hl.haxedefs), "hl builds for the host: -D windows -D desktop");
		assertEquals("hl", hl.conditions.get("targetType"), "hl target type");
		assertEquals("1", hl.conditions.get("hl"), "hl flag define");
		assertEquals("1", hl.conditions.get("windows"), "host platform define");
		assertEquals("windows", hl.conditions.get("target"), "target is the host");
		assertEquals("1", hl.conditions.get("native"), "native");

		var neko = TargetDefines.resolve("neko", "linux", false);
		assertEquals("desktop,linux", keys(neko.haxedefs), "neko on linux");
		assertEquals("neko", neko.conditions.get("targetType"), "neko target type");

		var cppOnMac = TargetDefines.resolve("cpp", "mac", false);
		assertEquals("desktop,mac,macos", keys(cppOnMac.haxedefs), "cpp on a mac host adds macos");
		assertEquals("1", cppOnMac.conditions.get("cpp"), "cpp flag");
	}

	static function mobileAndDebugSeeds():Void {
		var android = TargetDefines.resolve("android", "windows", false);
		assertEquals("android,mobile", keys(android.haxedefs), "android -D set");
		assertEquals("cpp", android.conditions.get("targetType"), "mobile builds are cpp");
		assertEquals("1", android.conditions.get("mobile"), "platform type define");

		var debug = TargetDefines.resolve("html5", "windows", true);
		assertEquals("debug", debug.conditions.get("buildType"), "debug build type");
		assertEquals("1", debug.conditions.get("debug"), "debug define");
		assertTrue(!debug.conditions.exists("release"), "no release define");
	}

	static function toolsDefineFollowsTheLimeHaxelib():Void {
		var resolvers = ProjectXmlEvaluator.resolversOf(null,
			(name, version) -> name == "lime" ? [library("lime", "8.3.2", "/lib/lime/8,3,2")] : null);
		var withLime = evaluate("<project/>", null, resolvers);
		withLime.defineToolsVersion();
		assertEquals("8.3.2", withLime.haxedefs.get("tools"), "tools is the lime version");

		var withoutLime = evaluate("<project/>");
		withoutLime.defineToolsVersion();
		assertTrue(!withoutLime.haxedefs.exists("tools"), "no lime, no tools define");
	}

	static function haxeVersionSeeds():Void {
		var seeds = ToolEnvironment.haxeVersionSeeds("4.3.7");
		assertEquals("4.3.7", seeds.get("haxe"), "haxe");
		assertEquals("4.3.7", seeds.get("haxe_ver"), "haxe_ver");
		assertEquals("1", seeds.get("haxe4"), "haxe<major> flag");
	}

	static function targetNamedEnvironmentVariablesAreDropped():Void {
		var environment = ToolEnvironment.withoutTargetNames(["windows" => "yes", "PATH" => "/bin"]);
		assertTrue(!environment.exists("windows"), "a target-named variable never passes a condition");
		assertEquals("/bin", environment.get("PATH"), "other variables stay");
	}

	/** A fake resolved library rooted at `root`, its classpath at root/src. **/
	static function library(name:String, version:String, root:String, ?includeXml:String,
			?extraDefines:Array<String>, ?extraArgs:Array<String>):ResolvedHaxelib {
		return {name: name, version: version, root: root, classpaths: [root + "/src"], includeXml: includeXml,
			extraDefines: extraDefines == null ? [] : extraDefines, extraArgs: extraArgs == null ? [] : extraArgs};
	}

	static function configValuesFlattenedToDotKeys():Void {
		// $$ = literal $ in a single-quoted haxe string (interpolation escape)
		var xml = '<project>
			<set name="dist" value="airdist"/>
			<config:air output-directory="$${dist}"/>
			<config><flash output-directory="swfdist"/></config>
			<config:html5><output-directory>web</output-directory></config:html5>
			<config:linux output-directory="lin" if="true"/>
			<config:windows output-directory="skipped" if="never"/>
		</project>';
		var result = evaluate(xml);
		assertEquals("airdist", result.config.get("air.output-directory"), "config: prefix form, variable substituted");
		assertEquals("swfdist", result.config.get("flash.output-directory"), "nested <config> form");
		assertEquals("web", result.config.get("html5.output-directory"), "text child form");
		assertEquals("lin", result.config.get("linux.output-directory"), "matched if keeps the value");
		assertTrue(!result.config.exists("linux.if"), "condition attributes are not values");
		assertTrue(!result.config.exists("windows.output-directory"), "unmatched if excludes the config");
	}

	static function hasLib(evaluator:ProjectXmlEvaluator, name:String):Bool {
		return Lambda.exists(evaluator.haxelibs, lib -> lib.name == name);
	}

	static function libVersion(evaluator:ProjectXmlEvaluator, name:String):Null<String> {
		var found = Lambda.find(evaluator.haxelibs, lib -> lib.name == name);
		return found == null ? null : found.version;
	}

	/** The map's keys, sorted and comma-joined, for one-line set assertions. **/
	static function keys(map:Map<String, String>):String {
		var names = [for (name in map.keys()) name];
		names.sort(Reflect.compare);
		return names.join(",");
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
