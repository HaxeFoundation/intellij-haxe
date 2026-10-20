package ijhaxe;

#if cpp
/**
	The runtime side of the injected profiling bootstrap: a start, and a stop
	that is safe to call twice. hxcpp's own stop crashes on a second call,
	and both the instrumented return from main and the instrumented
	System.exit may reach it. When the build has HXCPP_TELEMETRY and the IDE
	provided an endpoint, the telemetry collector streams alongside the
	text-report profiler.
**/
class ProfilerRun {
	static var stopped = false;

	public static function start(dumpFile:String):Void {
		cpp.vm.Profiler.start(dumpFile);
		#if (HXCPP_TELEMETRY && !HXCPP_TRACY)
		ijhaxe.TelemetryRun.tryStart();
		#end
	}

	public static function stopOnce():Void {
		if (stopped) return;
		stopped = true;
		#if (HXCPP_TELEMETRY && !HXCPP_TRACY)
		ijhaxe.TelemetryRun.stop();
		#end
		cpp.vm.Profiler.stop();
	}
}
#end
