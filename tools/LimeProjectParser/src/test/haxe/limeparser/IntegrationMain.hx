package limeparser;

import haxe.Json;
import haxe.io.Path;
import sys.FileSystem;
import sys.io.File;
import sys.io.Process;

/**
	End-to-end proof that BOTH project file kinds yield defines and libraries:
	writes a project.xml and a project.hxp fixture describing the same build,
	evaluates each (xml natively, hxp through the user's haxe with lime+hxp)
	and asserts the same conditional haxelib/haxedef outcome per target.

	Self-skips (exit 0 with a SKIPPED line) when the lime or hxp haxelib is not
	installed - the hxp path cannot run without them.
**/
class IntegrationMain {
	static var failures = 0;

	static function main():Void {
		if (!haxelibInstalled("lime") || !haxelibInstalled("hxp")) {
			Sys.println("SKIPPED: lime/hxp haxelib not installed - hxp evaluation needs both");
			return;
		}

		var directory = createTempDirectory();
		var xmlPath = Path.join([directory, "project.xml"]);
		var hxpPath = Path.join([directory, "project.hxp"]);
		File.saveContent(xmlPath, XML_FIXTURE);
		File.saveContent(hxpPath, HXP_FIXTURE);

		assertConfiguration("xml/hl", evaluateXml(xmlPath, ["hl" => ""]), "hlsteam", "webaudio");
		assertConfiguration("xml/html5", evaluateXml(xmlPath, ["html5" => ""]), "webaudio", "hlsteam");
		assertConfiguration("hxp/hl", evaluateHxp(hxpPath, "hl"), "hlsteam", "webaudio");
		assertConfiguration("hxp/html5", evaluateHxp(hxpPath, "html5"), "webaudio", "hlsteam");
		transitiveDependenciesResolve(xmlPath);

		if (failures > 0) {
			Sys.stderr().writeString(failures + " assertion(s) failed\n");
			Sys.exit(1);
		}
		Sys.println("LimeProjectParser integration tests passed (project.xml + project.hxp)");
	}

	// the same build described twice: openfl always, hlsteam only for hl,
	// webaudio only for html5, plus a fixture haxedef
	static final XML_FIXTURE = '<?xml version="1.0" encoding="utf-8"?>
<project>
	<meta title="Fixture"/>
	<app main="Main" path="Export" file="Fixture"/>
	<source path="src"/>
	<haxelib name="openfl"/>
	<haxelib name="hlsteam" version="1.0.0" if="hl"/>
	<haxelib name="webaudio" if="html5"/>
	<haxedef name="fixture-def" value="1"/>
</project>';

	static final HXP_FIXTURE = '
import hxp.*;
import lime.tools.*;

class Project extends HXProject {
	public function new() {
		super();
		meta.title = "Fixture";
		sources.push("src");
		haxelibs.push(new Haxelib("openfl"));
		if (target == Platform.HTML5) {
			haxelibs.push(new Haxelib("webaudio"));
		} else {
			haxelibs.push(new Haxelib("hlsteam", "1.0.0"));
		}
		haxedefs.set("fixture-def", "1");
	}
}';

	/** With the REAL haxelib resolver, declaring openfl must surface lime transitively (via openfl's include.xml). */
	static function transitiveDependenciesResolve(xmlPath:String):Void {
		if (!haxelibInstalled("openfl")) {
			Sys.println("openfl not installed - skipping the transitive resolution case");
			return;
		}
		var evaluator = new ProjectXmlEvaluator(["hl" => ""], "display", new Map(),
			ProjectXmlEvaluator.NO_INCLUDES, HaxelibLookup.resolver("haxelib"));
		evaluator.parse(File.getContent(xmlPath));
		if (!Lambda.exists(evaluator.haxelibs, lib -> lib.name == "lime")) {
			fail("transitive: lime not surfaced by openfl resolution");
		}
		if (!Lambda.exists(evaluator.sources, source -> source.indexOf("lime") >= 0)) {
			fail("transitive: lime classpath missing from sources");
		}
	}

	static function evaluateXml(path:String, seeds:Map<String, String>):Dynamic {
		var evaluator = new ProjectXmlEvaluator(seeds, "display", new Map(), ProjectXmlEvaluator.NO_INCLUDES);
		evaluator.parse(File.getContent(path));
		var haxedefs = {};
		for (name in evaluator.haxedefs.keys()) {
			Reflect.setField(haxedefs, name, evaluator.haxedefs.get(name));
		}
		return {haxedefs: haxedefs, haxelibs: evaluator.haxelibs, sources: evaluator.sources};
	}

	static function evaluateHxp(path:String, target:String):Dynamic {
		var json = HxpEvaluator.evaluate(path, target, new Map());
		return json == null ? null : Json.parse(json);
	}

	static function assertConfiguration(label:String, result:Dynamic, expectedLib:String, excludedLib:String):Void {
		if (result == null) {
			fail(label + ": evaluation returned nothing");
			return;
		}
		var libraries:Array<Dynamic> = result.haxelibs;
		if (!hasLib(libraries, "openfl")) fail(label + ": openfl missing");
		if (!hasLib(libraries, expectedLib)) fail(label + ": " + expectedLib + " missing");
		if (hasLib(libraries, excludedLib)) fail(label + ": " + excludedLib + " should be excluded");
		if (Reflect.field(result.haxedefs, "fixture-def") != "1") fail(label + ": fixture-def missing");
		var sources:Array<Dynamic> = result.sources;
		if (sources.indexOf("src") < 0) fail(label + ": src classpath missing");
	}

	static function hasLib(libraries:Array<Dynamic>, name:String):Bool {
		for (library in libraries) {
			if (library.name == name) return true;
		}
		return false;
	}

	static function fail(message:String):Void {
		Sys.stderr().writeString("FAILED: " + message + "\n");
		failures++;
	}

	static function haxelibInstalled(name:String):Bool {
		try {
			var process = new Process("haxelib", ["path", name]);
			process.stdout.readAll();
			process.stderr.readAll();
			var exitCode = process.exitCode();
			process.close();
			return exitCode == 0;
		} catch (e:Dynamic) {
			return false;
		}
	}

	static function createTempDirectory():String {
		var base = Sys.getEnv("TEMP");
		if (base == null) base = Sys.getEnv("TMPDIR");
		if (base == null) base = "/tmp";
		var directory = Path.join([base, "limeparser-it-" + Std.string(Std.random(0x7FFFFFFF))]);
		FileSystem.createDirectory(directory);
		return directory;
	}
}
