package limeparser;

import haxe.Json;
import haxe.io.Path;
import sys.FileSystem;
import sys.io.File;

/**
	The command-line entry point: evaluates a lime/openfl project file and
	prints the build configuration as JSON. A project.xml is evaluated
	directly. A project.hxp is a Haxe script, which the user's haxe runs with
	-lib lime -lib hxp (see HxpEvaluator).

	The caller passes the initial defines (target, platform, tool versions)
	as -D arguments; the tool does not derive them itself. Relative <include>
	paths resolve against the project file's directory.
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

		var projectFile:String = null;
		var command = "display";
		var target = "windows";
		var haxeExecutable = "haxe";
		var haxelibExecutable = "haxelib";
		var seedDefines:Map<String, String> = [];

		var i = 0;
		while (i < args.length) {
			var hasValue = i + 1 < args.length;
			switch (args[i]) {
				case "--command" if (hasValue): command = args[++i];
				case "--target" if (hasValue): target = args[++i];
				case "--haxe" if (hasValue): haxeExecutable = args[++i];
				case "--haxelib" if (hasValue): haxelibExecutable = args[++i];
				case "-D" if (hasValue): addDefine(seedDefines, args[++i]);
				case arg if (projectFile == null): projectFile = arg;
				case _:
			}
			i++;
		}

		if (projectFile == null || !FileSystem.exists(projectFile)) {
			Sys.stderr().writeString("project file not found: " + projectFile + "\n");
			Sys.exit(2);
		}

		if (Path.extension(projectFile).toLowerCase() == "hxp") {
			var json = HxpEvaluator.evaluate(projectFile, target, seedDefines, haxeExecutable);
			if (json == null) {
				Sys.exit(1);
			}
			Sys.println(json);
			return;
		}

		var projectDirectory = Path.directory(FileSystem.absolutePath(projectFile));
		var includeReader = readInclude.bind(projectDirectory);
		var haxelibResolver = HaxelibLookup.resolver(haxelibExecutable);
		var evaluator = new ProjectXmlEvaluator(seedDefines, command, Sys.environment(), includeReader, haxelibResolver);
		evaluator.parse(File.getContent(projectFile));

		Sys.println(Json.stringify({
			defines: mapToObject(evaluator.defines),
			haxedefs: mapToObject(evaluator.haxedefs),
			haxelibs: evaluator.haxelibs,
			sources: evaluator.sources,
			app: {path: evaluator.appPath, file: evaluator.appFile},
		}));
	}

	static function addDefine(defines:Map<String, String>, text:String):Void {
		var define = ProjectXmlEvaluator.splitDefine(text);
		defines.set(define.name, define.value);
	}

	/** An include path relative to the project directory; a directory stands for its include.xml. **/
	static function readInclude(projectDirectory:String, path:String):Null<String> {
		var resolved = Path.isAbsolute(path) ? path : Path.join([projectDirectory, path]);
		if (FileSystem.exists(resolved) && FileSystem.isDirectory(resolved)) {
			resolved = Path.join([resolved, "include.xml"]);
		}
		return FileSystem.exists(resolved) ? File.getContent(resolved) : null;
	}

	static function mapToObject(map:Map<String, String>):Dynamic {
		var object = {};
		for (name => value in map) {
			Reflect.setField(object, name, value);
		}
		return object;
	}
}
