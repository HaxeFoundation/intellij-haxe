package limeparser;

import haxe.Json;
import haxe.io.Path;
import limeparser.ProjectXmlEvaluator.Resolvers;
import sys.FileSystem;
import sys.io.File;

/** The command line, parsed. **/
private typedef Options = {
	projectFile:Null<String>,
	command:String,
	target:String,
	haxeExecutable:String,
	haxelibExecutable:String,
	seedDefines:Map<String, String>
}

/**
	The command-line entry point: evaluates a lime/openfl project file and
	prints the build configuration as JSON. A project.xml is evaluated
	directly. A project.hxp is a Haxe script, which the user's haxe runs with
	-lib lime -lib hxp (see HxpEvaluator).

	The target's own defines are derived from `--target` the way lime derives
	them (see TargetDefines); `-D` adds further condition defines. Relative
	<include> paths resolve against the including file's folder.
**/
class Main {
	static final USAGE = "usage: LimeProjectParser <project.xml|project.hxp> [--target <id>] [--command <cmd>]"
		+ " [--haxe <executable>] [--haxelib <executable>] [-D name[=value]]...\n";

	static function main():Void {
		var args = Sys.args();
		if (args.length == 0) {
			Sys.stderr().writeString(USAGE);
			Sys.exit(2);
		}
		var options = parseArguments(args);
		var projectFile = options.projectFile;
		if (projectFile == null || !FileSystem.exists(projectFile)) {
			Sys.stderr().writeString("project file not found: " + projectFile + "\n");
			Sys.exit(2);
		}

		var json = Path.extension(projectFile).toLowerCase() == "hxp"
			? HxpEvaluator.evaluate(projectFile, options.target, options.seedDefines, options.haxeExecutable)
			: evaluateXml(projectFile, options);
		if (json == null) {
			Sys.exit(1);
		}
		Sys.println(json);
	}

	static function parseArguments(args:Array<String>):Options {
		var options:Options = {
			projectFile: null,
			command: "display",
			target: "windows",
			haxeExecutable: "haxe",
			haxelibExecutable: "haxelib",
			seedDefines: []
		};
		var i = 0;
		while (i < args.length) {
			var hasValue = i + 1 < args.length;
			switch (args[i]) {
				case "--command" if (hasValue): options.command = args[++i];
				case "--target" if (hasValue): options.target = args[++i];
				case "--haxe" if (hasValue): options.haxeExecutable = args[++i];
				case "--haxelib" if (hasValue): options.haxelibExecutable = args[++i];
				case "-D" if (hasValue): addDefine(options.seedDefines, args[++i]);
				case arg if (options.projectFile == null): options.projectFile = arg;
				case _:
			}
			i++;
		}
		return options;
	}

	/**
		lime's sequence: the target's condition defines seed the parser, the
		project is parsed, then the build adds tools=<lime version> and the
		target's own -D flags on top of the project's haxedefs.
	**/
	static function evaluateXml(projectFile:String, options:Options):String {
		var projectDirectory = Path.directory(FileSystem.absolutePath(projectFile));
		var host = TargetDefines.hostPlatform(Sys.systemName());
		var targetDefines = TargetDefines.resolve(options.target, host, options.seedDefines.exists("debug"));
		for (name => value in options.seedDefines) {
			targetDefines.conditions.set(name, value);
		}
		var environment = ToolEnvironment.build(options.haxeExecutable);
		var resolvers:Resolvers = {
			include: IncludeFiles.resolve.bind(projectDirectory),
			haxelib: HaxelibLookup.resolver(options.haxelibExecutable),
			localHaxelib: HaxelibLookup.local.bind(projectDirectory)
		};

		var evaluator = new ProjectXmlEvaluator(targetDefines.conditions, options.command, environment, projectDirectory, resolvers);
		evaluator.parse(File.getContent(projectFile));
		evaluator.defineToolsVersion();
		for (name => value in targetDefines.haxedefs) {
			evaluator.haxedefs.set(name, value);
		}

		return Json.stringify({
			defines: mapToObject(evaluator.defines),
			haxedefs: mapToObject(evaluator.haxedefs),
			haxelibs: evaluator.haxelibs,
			sources: evaluator.sources,
			app: {path: evaluator.appPath, file: evaluator.appFile},
		});
	}

	static function addDefine(defines:Map<String, String>, text:String):Void {
		var define = ProjectXmlEvaluator.splitDefine(text);
		defines.set(define.name, define.value);
	}

	static function mapToObject(map:Map<String, String>):Dynamic {
		var object = {};
		for (name => value in map) {
			Reflect.setField(object, name, value);
		}
		return object;
	}
}
