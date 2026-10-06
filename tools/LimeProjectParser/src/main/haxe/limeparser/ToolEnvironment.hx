package limeparser;

import sys.io.Process;

/**
	The environment lime's parser consults for conditions and ${} values:
	the process environment without the names that double as target
	conditions, plus the haxe version seeds (haxe, haxe_ver, haxe<major>)
	read from `haxe -version`, as CommandLineTools builds it.
**/
class ToolEnvironment {
	// a variable named like a target would otherwise pass if="windows" on every build
	static final TARGET_NAMES = [
		"air", "android", "cpp", "flash", "hl", "html5", "ios", "linux", "mac", "neko", "webassembly", "windows"
	];

	public static function build(haxeExecutable:String):Map<String, String> {
		var environment = withoutTargetNames(Sys.environment());
		var version = haxeVersion(haxeExecutable);
		if (version != null) {
			for (name => value in haxeVersionSeeds(version)) {
				environment.set(name, value);
			}
		}
		return environment;
	}

	public static function withoutTargetNames(environment:Map<String, String>):Map<String, String> {
		for (name in TARGET_NAMES) {
			environment.remove(name);
		}
		return environment;
	}

	/** haxe and haxe_ver carry the full version; haxe<major> (haxe4) is a flag. **/
	public static function haxeVersionSeeds(version:String):Map<String, String> {
		return ["haxe" => version, "haxe_ver" => version, "haxe" + version.split(".")[0] => "1"];
	}

	/** `haxe -version` prints to stderr on older releases and to stdout on current ones; null when it cannot run. **/
	static function haxeVersion(haxeExecutable:String):Null<String> {
		try {
			var process = new Process(haxeExecutable, ["-version"]);
			var version = StringTools.trim(process.stderr.readAll().toString());
			if (version == "") {
				version = StringTools.trim(process.stdout.readAll().toString());
			}
			process.close();
			return version == "" ? null : version;
		} catch (e:Dynamic) {
			return null;
		}
	}
}
