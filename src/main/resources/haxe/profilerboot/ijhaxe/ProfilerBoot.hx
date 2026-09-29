package ijhaxe;

#if macro
import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;

/**
	An initialization macro that wraps ONLY the main class's `main` with a
	profiler start and stop, so a build is profiled without source changes:
	`--macro ijhaxe.ProfilerBoot.use('run.hxcppprof')`. Lime applications
	never return from main; they always shut down through
	`lime.system.System.exit`, so that method gets a stop prepended too. In
	builds without lime, that metadata matches no type and has no effect.
**/
class ProfilerBoot {
	public static function use(dumpFile:String) {
		// Compiler.getConfiguration, which names the main class, exists since haxe 4.3
		#if (haxe_ver >= 4.3)
		var main = Compiler.getConfiguration().mainClass;
		if (main == null) {
			Context.warning("ProfilerBoot: no main class - profiler not injected", Context.currentPos());
			return;
		}
		var qualified = main.pack.length > 0 ? main.pack.join(".") + "." + main.name : main.name;
		Compiler.addGlobalMetadata(qualified,
			'@:build(ijhaxe.ProfilerBoot.instrument("' + dumpFile + '"))', true, true, false);
		Compiler.addGlobalMetadata("lime.system.System",
			"@:build(ijhaxe.ProfilerBoot.instrumentExit())", true, true, false);
		#else
		Context.warning("ProfilerBoot: profiling injection needs haxe 4.3+ - profiler not injected", Context.currentPos());
		#end
	}

	public static function instrument(dumpFile:String):Array<Field> {
		var fields = Context.getBuildFields();
		for (field in fields) {
			if (field.name != "main") continue;
			switch (field.kind) {
				case FFun(fn) if (fn.expr != null):
					var body = fn.expr;
					// the body runs inside a closure so an early `return` still reaches stop
					fn.expr = macro {
						ijhaxe.ProfilerRun.start($v{dumpFile});
						var run = function() $b{[body]};
						try {
							run();
						} catch (e:Dynamic) {
							ijhaxe.ProfilerRun.stopOnce();
							throw e;
						}
						ijhaxe.ProfilerRun.stopOnce();
					};
				default:
			}
		}
		return fields;
	}

	public static function instrumentExit():Array<Field> {
		var fields = Context.getBuildFields();
		for (field in fields) {
			if (field.name != "exit") continue;
			switch (field.kind) {
				case FFun(fn) if (fn.expr != null):
					var body = fn.expr;
					fn.expr = macro {
						ijhaxe.ProfilerRun.stopOnce();
						$b{[body]};
					};
				default:
			}
		}
		return fields;
	}
}
#end
