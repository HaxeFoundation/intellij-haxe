package ijhaxe.hxcpp.debug;

typedef ServerConfig = {
	var host:String;
	var port:Int;
	// True when ANY environment variable or define supplied a value, even an
	// invalid one. Someone asked for debugging, so the server retries the
	// connection patiently; an unconfigured build makes one quick attempt and
	// then runs on.
	var configured:Bool;
}

/**
	Resolves the connection settings. Environment variables come first: the
	IDE sets `HXCPP_DEBUG_HOST`/`HXCPP_DEBUG_PORT` on the spawned process with
	a fresh port per session, so ports never collide and need no rebuild.
	Next come the compile-time defines, which use the same names as the
	vshaxe debug server's; last the defaults. The environment reader is a
	parameter, so the unit tests run under the interpreter.
**/
class Config {
	public static inline var DEFAULT_HOST = "127.0.0.1";
	public static inline var DEFAULT_PORT = 6972;

	public static function resolve(env:String->Null<String>, ?defineHost:String, ?definePort:String):ServerConfig {
		var envHost = env("HXCPP_DEBUG_HOST");
		var envPort = env("HXCPP_DEBUG_PORT");
		var host = nonEmpty(envHost);
		if (host == null) {
			host = nonEmpty(defineHost);
		}
		var port = parsePort(envPort);
		if (port == null) {
			port = parsePort(definePort);
		}
		return {
			host: host != null ? host : DEFAULT_HOST,
			port: port != null ? port : DEFAULT_PORT,
			configured: nonEmpty(envHost) != null || nonEmpty(envPort) != null
				|| nonEmpty(defineHost) != null || nonEmpty(definePort) != null
		};
	}

	static function nonEmpty(value:Null<String>):Null<String> {
		if (value == null) {
			return null;
		}
		var trimmed = StringTools.trim(value);
		return trimmed == "" ? null : trimmed;
	}

	// Null unless a valid TCP port (1..65535), so a typo falls through to the
	// next source instead of aiming the server at port 0 or garbage.
	static function parsePort(value:Null<String>):Null<Int> {
		if (value == null) {
			return null;
		}
		var parsed = Std.parseInt(StringTools.trim(value));
		return parsed != null && parsed > 0 && parsed <= 65535 ? parsed : null;
	}
}
