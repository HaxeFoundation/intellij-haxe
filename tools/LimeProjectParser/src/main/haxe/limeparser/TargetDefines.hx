package limeparser;

/** What a target id stands for: the platform lime builds for, the target flags it sets and the haxedefs it adds. **/
private typedef Target = {platform:String, flags:Array<String>, haxedefs:Array<String>}

/**
	The two define sets a target yields: `conditions` are seeded before the
	project is parsed, `haxedefs` are the -D flags the build adds beyond the
	project's own.
**/
typedef TargetDefineSets = {conditions:Map<String, String>, haxedefs:Map<String, String>}

/**
	The defines lime derives from a target id alone, with no project file
	involved: the condition defines HXProject.initializeDefines seeds before
	parsing (html5=1, platformType=web, buildType=release, ...) and the -D flags
	the build adds beyond the project's haxedefs (-D html5 -D web; -D macos
	for mac). Target ids follow CommandLineTools: hl, neko, cppia, java,
	nodejs and cs are flags on the HOST platform, and aliases such as
	hashlink or macos map to their platform.
**/
class TargetDefines {
	/** The pseudo targets initializeDefines probes, in its order; each sets targetType, native and its own name. **/
	static final PSEUDO_TARGETS = ["neko", "hl", "java", "nodejs", "cs"];
	static final WEB_PLATFORMS = ["flash", "html5", "firefox", "webassembly"];
	static final MOBILE_PLATFORMS = ["android", "blackberry", "ios", "tizen", "webos", "tvos"];
	static final DESKTOP_PLATFORMS = ["windows", "mac", "linux", "air"];
	static final WEBASSEMBLY_DEFINES = [
		"webassembly" => "1", "wasm" => "1", "emscripten" => "1", "targetType" => "cpp", "native" => "1", "cpp" => "1"
	];

	public static function resolve(targetId:String, host:String, debug:Bool):TargetDefineSets {
		var target = targetOf(targetId, host);
		var platformType = platformTypeOf(target);
		return {
			conditions: conditionDefines(target, platformType, host, debug),
			haxedefs: compilerDefines(target, platformType)
		};
	}

	/** The host platform id lime uses, from `Sys.systemName()`. **/
	public static function hostPlatform(systemName:String):String {
		return switch (systemName) {
			case "Windows": "windows";
			case "Mac": "mac";
			default: "linux";
		}
	}

	/** CommandLineTools' target-name switch. **/
	static function targetOf(targetId:String, host:String):Target {
		return switch (targetId) {
			case "cpp": target(host, ["cpp"], host == "mac" ? ["macos"] : []);
			case "neko": target(host, ["neko"]);
			case "hl", "hashlink": target(host, ["hl"]);
			case "hlc": target(host, ["hl", "hlc"]);
			case "cppia": target(host, ["cppia"]);
			case "java": target(host, ["java"]);
			case "nodejs": target(host, ["nodejs"]);
			case "cs": target(host, ["cs"]);
			case "iphone", "iphoneos": target("ios");
			case "iphonesim": target("ios", ["simulator"]);
			case "electron": target("html5", ["electron"]);
			case "firefox", "firefoxos": target("firefox", [], ["firefoxos"]);
			case "mac", "macos": target("mac", [], ["macos"]);
			case "rpi", "raspberrypi": target("linux", ["rpi"]);
			case "webassembly", "wasm", "emscripten": target("webassembly", ["webassembly"]);
			case "winjs", "uwp": target("windows", ["uwp", "winjs"]);
			case "winrt": target("windows", ["winrt"]);
			default: target(targetId.toLowerCase());
		}
	}

	static function target(platform:String, ?flags:Array<String>, ?haxedefs:Array<String>):Target {
		return {platform: platform, flags: flags == null ? [] : flags, haxedefs: haxedefs == null ? [] : haxedefs};
	}

	/** HXProject's platformType switch; an unknown platform counts as a console. **/
	static function platformTypeOf(target:Target):String {
		var airMobile = target.platform == "air" && (target.flags.contains("ios") || target.flags.contains("android"));
		if (airMobile) return "mobile";
		if (WEB_PLATFORMS.contains(target.platform)) return "web";
		if (MOBILE_PLATFORMS.contains(target.platform)) return "mobile";
		if (DESKTOP_PLATFORMS.contains(target.platform)) return "desktop";
		return "console";
	}

	/** HXProject.initializeDefines, in its order. **/
	static function conditionDefines(target:Target, platformType:String, host:String, debug:Bool):Map<String, String> {
		var defines:Map<String, String> = [];
		defines.set("platformType", platformType);
		defines.set(platformType, "1");
		for (name => value in targetTypeDefines(target, platformType, host)) {
			defines.set(name, value);
		}

		var buildType = buildTypeOf(target.flags, debug);
		defines.set("buildType", buildType);
		defines.set(buildType, "1");
		if (target.flags.contains("static")) defines.set("static_link", "1");

		defines.set(target.platform, "1");
		defines.set("target", target.platform);
		defines.set("platform", target.platform);
		defines.set("host", host);
		defines.set("lime-tools", "1");
		defines.set("hxp", "1");
		return defines;
	}

	/** The one targetType branch that applies, probed in initializeDefines' order. **/
	static function targetTypeDefines(target:Target, platformType:String, host:String):Map<String, String> {
		var platform = target.platform;
		var flags = target.flags;
		var pseudo = Lambda.find(PSEUDO_TARGETS, flags.contains);
		if (pseudo != null) return pseudoTargetDefines(pseudo, flags);
		if (platform == "firefox") return ["targetType" => "js", "html5" => "1"];
		if (platform == "air") return airDefines(flags);
		if (platform == "windows" && (flags.contains("uwp") || flags.contains("winjs"))) {
			return ["targetType" => "js", "html5" => "1", "uwp" => "1", "winjs" => "1"];
		}
		// a desktop platform other than the host is cross-compiled: cpp only
		// when the cpp (or mingw) flag asks for it, else a neko build
		if (platformType == "desktop" && platform != host) return crossDesktopDefines(platform, flags);
		if (platform == "webassembly") return WEBASSEMBLY_DEFINES;
		if (flags.contains("cpp") || (platformType != "web" && !flags.contains("html5"))) {
			return ["targetType" => "cpp", "native" => "1", "cpp" => "1"];
		}
		if (platform == "flash") return ["targetType" => "swf"];
		return [];
	}

	static function pseudoTargetDefines(pseudo:String, flags:Array<String>):Map<String, String> {
		var defines = ["targetType" => pseudo, "native" => "1", pseudo => "1"];
		if (pseudo == "hl" && flags.contains("hlc")) defines.set("hlc", "1");
		return defines;
	}

	static function airDefines(flags:Array<String>):Map<String, String> {
		var defines = ["targetType" => "swf", "flash" => "1"];
		if (flags.contains("ios")) defines.set("ios", "1");
		if (flags.contains("android")) defines.set("android", "1");
		return defines;
	}

	static function crossDesktopDefines(platform:String, flags:Array<String>):Map<String, String> {
		var defines = ["native" => "1"];
		if (platform == "linux" && flags.contains("cpp")) {
			defines.set("targetType", "cpp");
			defines.set("cpp", "1");
		} else if (platform == "windows" && (flags.contains("cpp") || flags.contains("mingw"))) {
			defines.set("targetType", "cpp");
			defines.set("cpp", "1");
			defines.set("mingw", "1");
		} else {
			defines.set("targetType", "neko");
			defines.set("neko", "1");
		}
		return defines;
	}

	static function buildTypeOf(flags:Array<String>, debug:Bool):String {
		if (debug) return "debug";
		if (flags.contains("final")) return "final";
		return "release";
	}

	/** The -D flags HXProject adds after the haxedefs: the platform (never for flash), its type, and the haxedefs the target id itself implies (macos, firefoxos). **/
	static function compilerDefines(target:Target, platformType:String):Map<String, String> {
		var defines:Map<String, String> = [];
		for (name in target.haxedefs) {
			defines.set(name, "");
		}
		if (target.platform != "flash") defines.set(target.platform, "");
		defines.set(platformType, "");
		return defines;
	}
}
