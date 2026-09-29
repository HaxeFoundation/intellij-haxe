// Haxelib lives in the hxp package (lime imports hxp.*); Platform in lime.tools
import haxe.Json;
import lime.tools.HXProject;
import lime.tools.Platform;
import hxp.Haxelib;

/**
	Runs under the USER's haxe with -lib lime -lib hxp. It is never compiled
	into the tool's jar, because lime.tools is available only in that
	context. It mirrors lime.tools.HXProject.main: it sets the HXProject
	statics, instantiates the .hxp script class, and prints the build
	configuration as JSON instead of a serialized HXProject.

	Usage (spawned by HxpEvaluator):
	  haxe <ScriptClass> -cp <temp> -lib lime -lib hxp --run HxpRunner <ScriptClass> --target <id> [-D name[=value]]...

	Whatever haxe the user has compiles it, so it keeps to the 4.1 syntax floor.
**/
class HxpRunner {
	public static function main():Void {
		var args = Sys.args();
		var className = args[0];
		// --target is always passed by HxpEvaluator; the fallback is the host platform
		var targetId = switch (Sys.systemName()) {
			case "Windows": "windows";
			case "Mac": "mac";
			default: "linux";
		};
		var userDefines = new Map<String, Dynamic>();

		var i = 1;
		while (i < args.length) {
			var arg = args[i];
			if (arg == "--target" && i + 1 < args.length) {
				targetId = args[++i];
			} else if (arg == "-D" && i + 1 < args.length) {
				var pair = args[++i].split("=");
				userDefines.set(pair[0], pair.length > 1 ? pair.slice(1).join("=") : "");
			}
			i++;
		}

		HXProject._command = "display";
		HXProject._debug = userDefines.exists("debug");
		HXProject._targetFlags = new Map();
		HXProject._userDefines = userDefines;
		HXProject._environment = Sys.environment();
		HXProject._templatePaths = [];
		HXProject._target = resolvePlatform(targetId, HXProject._targetFlags);

		var scriptClass = Type.resolveClass(className);
		if (scriptClass == null) {
			Sys.stderr().writeString("script class not found: " + className + "\n");
			Sys.exit(3);
		}
		var project:HXProject = cast Type.createInstance(scriptClass, []);

		var haxedefs = {};
		var defines = {};
		for (name in project.haxedefs.keys()) {
			var value = Std.string(project.haxedefs.get(name));
			Reflect.setField(haxedefs, name, value);
			Reflect.setField(defines, name, value);
		}
		for (name in userDefines.keys()) {
			Reflect.setField(defines, name, Std.string(userDefines.get(name)));
		}

		var haxelibs = [];
		for (haxelib in project.haxelibs) {
			haxelibs.push({name: haxelib.name, version: haxelib.version == null ? "" : haxelib.version});
			// lime's project.xml parser defines each haxelib's name; do the same
			if (!Reflect.hasField(defines, haxelib.name)) {
				Reflect.setField(defines, haxelib.name, haxelib.version == null ? "" : haxelib.version);
			}
		}

		Sys.println(Json.stringify({
			defines: defines,
			haxedefs: haxedefs,
			haxelibs: haxelibs,
			sources: project.sources,
			app: {
				path: project.app.path == null ? "bin" : project.app.path,
				file: project.app.file == null ? "" : project.app.file
			},
		}));
	}

	/**
		Maps a lime command-line target id to the Platform the script sees. The
		compile targets that are not platforms of their own (hl, neko, cppia,
		java, cs, nodejs) run on the HOST platform with a target flag set, as
		lime's CommandLineTools maps them.
	**/
	static function resolvePlatform(targetId:String, targetFlags:Map<String, String>):Platform {
		switch (targetId) {
			case "html5": return Platform.HTML5;
			case "flash": return Platform.FLASH;
			case "air": return Platform.AIR;
			case "android": return Platform.ANDROID;
			case "ios": return Platform.IOS;
			case "windows": return Platform.WINDOWS;
			case "mac": return Platform.MAC;
			case "linux": return Platform.LINUX;
			default:
				targetFlags.set(targetId, "");
				switch (Sys.systemName()) {
					case "Windows": return Platform.WINDOWS;
					case "Mac": return Platform.MAC;
					default: return Platform.LINUX;
				}
		}
	}
}
