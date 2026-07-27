package tests;

import ijhaxe.hxcpp.debug.Config;

class ConfigTest {
	public static function run(assert:Assert):Void {
		envVarsWinOverDefinesAndDefaults(assert);
		definesFillInWhenEnvIsAbsent(assert);
		defaultsApplyWhenNothingIsSet(assert);
		invalidOrEmptyValuesFallThroughTheChain(assert);
	}

	static function env(values:Map<String, String>):String->Null<String> {
		return name -> values.get(name);
	}

	static function envVarsWinOverDefinesAndDefaults(assert:Assert):Void {
		var config = Config.resolve(env(["HXCPP_DEBUG_HOST" => "10.0.0.5", "HXCPP_DEBUG_PORT" => "49321"]), "definehost", "7000");
		assert.equals("10.0.0.5", config.host, "env host wins");
		assert.equals(49321, config.port, "env port wins");
	}

	static function definesFillInWhenEnvIsAbsent(assert:Assert):Void {
		var config = Config.resolve(env(new Map()), "definehost", "7000");
		assert.equals("definehost", config.host, "define host used without env");
		assert.equals(7000, config.port, "define port used without env");
	}

	static function defaultsApplyWhenNothingIsSet(assert:Assert):Void {
		var config = Config.resolve(env(new Map()));
		assert.equals(Config.DEFAULT_HOST, config.host, "default host");
		assert.equals(Config.DEFAULT_PORT, config.port, "default port");
		assert.isTrue(!config.configured, "nothing set = not configured (one quick connect attempt only)");
		assert.isTrue(Config.resolve(env(["HXCPP_DEBUG_PORT" => "6001"])).configured, "env port marks configured");
		assert.isTrue(Config.resolve(env(new Map()), null, "garbage").configured,
			"even an invalid define marks configured (someone asked for debugging)");
	}

	static function invalidOrEmptyValuesFallThroughTheChain(assert:Assert):Void {
		var config = Config.resolve(env(["HXCPP_DEBUG_HOST" => "  ", "HXCPP_DEBUG_PORT" => "notaport"]), "definehost", "0");
		assert.equals("definehost", config.host, "blank env host falls through to the define");
		assert.equals(Config.DEFAULT_PORT, config.port, "bad env port and port-0 define both fall through to the default");

		var trimmed = Config.resolve(env(["HXCPP_DEBUG_PORT" => " 6001 "]));
		assert.equals(6001, trimmed.port, "surrounding whitespace is tolerated");

		var tooBig = Config.resolve(env(["HXCPP_DEBUG_PORT" => "70000"]));
		assert.equals(Config.DEFAULT_PORT, tooBig.port, "out-of-range port falls through");
	}
}
