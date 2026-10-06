package limeparser;

import haxe.Json;
import haxe.io.Path;
import limeparser.ProjectXmlEvaluator.ResolvedHaxelib;
import sys.FileSystem;
import sys.io.File;
import sys.io.Process;

/**
	Resolves a haxelib and its transitive dependencies by parsing the output
	of `haxelib path <name[:version]>`. haxelib prints the libraries in
	dependency order. For each one it prints any `-L` ndll lines, the content
	of its extraParams.hxml INLINE, one or more bare classpath lines, and
	finally a `-D name=version` line that closes the library. A -D line seen
	before any classpath therefore comes from extraParams, not from that
	closing line. Each library root is also checked for an include file.
**/
class HaxelibLookup {
	public static function resolver(haxelibExecutable:String):(String, String) -> Null<Array<ResolvedHaxelib>> {
		var cache:Map<String, Null<Array<ResolvedHaxelib>>> = [];
		return (name, version) -> {
			var spec = version == "" ? name : name + ":" + version;
			if (!cache.exists(spec)) {
				cache.set(spec, resolve(haxelibExecutable, spec));
			}
			return cache.get(spec);
		};
	}

	/**
		A library checked out at a folder (`<haxelib path>`), relative to the
		project directory: version and classpath from its haxelib.json, the
		include file per lime's order. Null when the folder is missing.
	**/
	public static function local(projectDirectory:String, name:String, root:String):Null<ResolvedHaxelib> {
		var location = Path.isAbsolute(root) ? root : Path.join([projectDirectory, root]);
		if (!FileSystem.exists(location) || !FileSystem.isDirectory(location)) return null;

		var json = haxelibJson(location);
		var version = json == null || json.version == null ? "" : Std.string(json.version);
		var classpath = json == null || json.classPath == null ? location : Path.join([location, json.classPath]);
		return {name: name, version: version, root: location, classpaths: [classpath],
			includeXml: IncludeFiles.contentIn(location), extraDefines: [], extraArgs: []};
	}

	static function resolve(haxelibExecutable:String, spec:String):Null<Array<ResolvedHaxelib>> {
		var output:String;
		try {
			var process = new Process(haxelibExecutable, ["path", spec]);
			output = process.stdout.readAll().toString();
			process.stderr.readAll();
			var exitCode = process.exitCode();
			process.close();
			if (exitCode != 0) return null;
		} catch (e:Dynamic) {
			return null;
		}
		return parseOutput(output);
	}

	/** Parses `haxelib path` output; kept apart from the process call so tests can feed it text. **/
	public static function parseOutput(output:String):Array<ResolvedHaxelib> {
		var libraries:Array<ResolvedHaxelib> = [];
		var pendingClasspaths:Array<String> = [];
		var pendingExtraDefines:Array<String> = [];
		var pendingExtraArgs:Array<String> = [];
		for (rawLine in output.split("\n")) {
			var line = StringTools.trim(rawLine);
			if (line == "") continue;

			if (StringTools.startsWith(line, "-D ")) {
				if (pendingClasspaths.length > 0) {
					// the version line (-D name=1.2.3) closes the library
					var pair = line.substr(3).split("=");
					libraries.push(makeLibrary(pair[0], pair.length > 1 ? pair[1] : "",
						pendingClasspaths, pendingExtraDefines, pendingExtraArgs));
					pendingClasspaths = [];
					pendingExtraDefines = [];
					pendingExtraArgs = [];
				} else {
					// before any classpath: a define from the library's extraParams.hxml
					pendingExtraDefines.push(StringTools.trim(line.substr(3)));
				}
			} else if (StringTools.startsWith(line, "-")) {
				pendingExtraArgs.push(line);
			} else {
				pendingClasspaths.push(Path.removeTrailingSlashes(line));
			}
		}
		return libraries;
	}

	static function makeLibrary(name:String, version:String, classpaths:Array<String>,
			extraDefines:Array<String>, extraArgs:Array<String>):ResolvedHaxelib {
		// lime takes the classpath printed right before the -D marker as the library's path
		var root = classpaths.length > 0 ? libraryRoot(classpaths[classpaths.length - 1]) : "";
		var includeXml = root == "" ? null : IncludeFiles.contentIn(root);
		return {name: name, version: version, root: root, classpaths: classpaths,
			includeXml: includeXml, extraDefines: extraDefines, extraArgs: extraArgs};
	}

	/** lime's Haxelib.getPath: the first folder at or above the classpath holding a haxelib.json, else the classpath. **/
	public static function libraryRoot(classpath:String):String {
		var folder = classpath;
		while (folder != "") {
			if (FileSystem.exists(Path.join([folder, "haxelib.json"]))) return folder;
			folder = Path.directory(folder);
		}
		return classpath;
	}

	static function haxelibJson(root:String):Null<Dynamic> {
		var path = Path.join([root, "haxelib.json"]);
		if (!FileSystem.exists(path)) return null;
		return try Json.parse(File.getContent(path)) catch (e:Dynamic) null;
	}
}
