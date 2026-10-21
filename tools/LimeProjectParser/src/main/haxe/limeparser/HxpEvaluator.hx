package limeparser;

import haxe.Resource;
import haxe.io.Path;
import sys.FileSystem;
import sys.io.File;
import sys.io.Process;
import sys.thread.Thread;

/**
	Evaluates a .hxp project script. The script is arbitrary Haxe code
	extending lime.tools.HXProject, so it has to RUN; parsing is not enough.
	The steps mirror lime's HXProject.fromFile: the script is copied to a
	temp directory as <Name>.hx (the class name is the capitalized file
	name), its @:compiler( lines become extra compiler arguments, and the
	user's haxe runs it with -lib lime -lib hxp. lime serializes the project
	and reads it back; instead, the bundled HxpRunner, extracted beside the
	script, prints the configuration as JSON from inside that run.

	Requires haxe and the lime and hxp haxelibs. Returns null, with a message
	on stderr, when evaluation fails.
**/
class HxpEvaluator {
	public static function evaluate(hxpPath:String, target:String, defines:Map<String, String>,
			haxeExecutable:String = "haxe"):Null<String> {
		// Haxe has no finally. The temp directory holds a copy of the user's
		// script, so errors are caught and the directory is deleted either way.
		var tempDirectory = createTempDirectory();

		var result = try {
			evaluateIn(tempDirectory, hxpPath, target, defines, haxeExecutable);
		} catch (e:Dynamic) {
			Sys.stderr().writeString("hxp evaluation failed: " + Std.string(e) + "\n");
			null;
		}

		deleteDirectory(tempDirectory);
		return result;
	}

	static function evaluateIn(tempDirectory:String, hxpPath:String, target:String, defines:Map<String, String>,
			haxeExecutable:String):Null<String> {
		var absolute = FileSystem.absolutePath(hxpPath);
		var name = className(hxpPath);

		File.saveContent(Path.join([tempDirectory, name + ".hx"]), File.getContent(absolute));
		File.saveContent(Path.join([tempDirectory, "HxpRunner.hx"]), Resource.getString("HxpRunner.hx"));

		var args = [name, "-lib", "lime", "-lib", "hxp", "-cp", tempDirectory];
		for (line in compilerLines(absolute)) {
			args = args.concat(line);
		}
		args = args.concat(["--run", "HxpRunner", name, "--target", target]);
		for (define in defines.keys()) {
			var value = defines.get(define);
			args.push("-D");
			args.push(value == "" ? define : define + "=" + value);
		}

		var process = new Process(haxeExecutable, args);

		// Reading stdout to EOF before stderr deadlocks when the child fills the
		// stderr pipe buffer: the child blocks writing while this process blocks
		// reading stdout. A helper thread therefore drains stderr concurrently,
		// and the blocking readMessage waits for it before exitCode().
		var main = Thread.current();
		Thread.create(() -> {
			var text = try process.stderr.readAll().toString() catch (e:Dynamic) "";
			main.sendMessage(text);
		});
		var stdout = process.stdout.readAll().toString();
		var stderr:String = Thread.readMessage(true);

		var exitCode = process.exitCode();
		process.close();

		if (exitCode != 0) {
			Sys.stderr().writeString("hxp evaluation failed (exit " + exitCode + "):\n" + stderr);
			return null;
		}
		// compiler output may precede the runner's; the JSON is the last line
		var lines = StringTools.trim(stdout).split("\n");
		return StringTools.trim(lines[lines.length - 1]);
	}

	/** lime's naming rule: the class is the capitalized file name without extension. **/
	public static function className(hxpPath:String):String {
		var name = Path.withoutDirectory(Path.withoutExtension(hxpPath));
		return name.charAt(0).toUpperCase() + name.substr(1);
	}

	/** @:compiler("...") lines in the script carry extra compiler arguments (lime convention). **/
	static function compilerLines(path:String):Array<Array<String>> {
		var result = [];
		var tag = "@:compiler(";
		for (line in File.getContent(path).split("\n")) {
			var trimmed = StringTools.trim(line);
			if (StringTools.startsWith(trimmed, tag)) {
				// strips @:compiler(" and "); the payload is a quoted argument string
				var payload = trimmed.substring(tag.length + 1, trimmed.length - 2);
				result.push(payload.split(" "));
			}
		}
		return result;
	}

	static function createTempDirectory():String {
		var base = Sys.getEnv("TEMP");
		if (base == null) base = Sys.getEnv("TMPDIR");
		if (base == null) base = "/tmp";
		var directory = Path.join([base, "limeprojectparser-" + Std.string(Std.random(0x7FFFFFFF))]);
		FileSystem.createDirectory(directory);
		return directory;
	}

	/** Best-effort recursive delete; a cleanup failure must not mask the evaluation result. */
	static function deleteDirectory(directory:String):Void {
		try {
			for (entry in FileSystem.readDirectory(directory)) {
				var path = Path.join([directory, entry]);
				if (FileSystem.isDirectory(path)) {
					deleteDirectory(path);
				} else {
					FileSystem.deleteFile(path);
				}
			}
			FileSystem.deleteDirectory(directory);
		} catch (e:Dynamic) {
			Sys.stderr().writeString("warning: could not delete temp directory " + directory + "\n");
		}
	}
}
