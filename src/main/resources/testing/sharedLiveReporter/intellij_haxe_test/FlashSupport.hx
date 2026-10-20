package intellij_haxe_test;

#if flash
/**
	Flash-family shims for adl-hosted test runs: adl (with -nodebug) only
	forwards NATIVE trace output to stdout, and only an explicit exit call
	ends the hosting process.
**/
class FlashSupport {
	/** Routes `trace()` to the native trace; haxe's flash default draws into an on-screen TextField instead. */
	public static function hookTrace():Void {
		haxe.Log.trace = function(v:Dynamic, ?pos:haxe.PosInfos) flash.Lib.trace(Std.string(v));
	}

	/**
		Ends the hosting process: NativeApplication under adl/AIR (where
		System.exit throws SecurityError #2018 - it is the STANDALONE-player
		API), System.exit in the standalone player, no-op in a browser plugin.
	**/
	public static function exit(code:Int):Void {
		try {
			var nativeApp:Dynamic = untyped __global__["flash.desktop.NativeApplication"];
			if (nativeApp != null && nativeApp.nativeApplication != null) {
				nativeApp.nativeApplication.exit(code);
				return;
			}
		} catch (e:Dynamic) {}
		try flash.system.System.exit(code) catch (e:Dynamic) {}
	}
}
#end
