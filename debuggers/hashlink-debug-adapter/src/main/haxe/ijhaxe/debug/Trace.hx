package ijhaxe.debug;

/**
	Diagnostic lines on stderr for finding hangs and lost messages, enabled by
	the DAP_ADAPTER_TRACE environment variable. The integration tests set it and
	print the output on teardown, so the last line of a hung run shows where it
	stopped.
**/
class Trace {
	public static final ENABLED = Sys.getEnv("DAP_ADAPTER_TRACE") != null;

	/**
		Guards hot call sites so they skip building the message when tracing is
		off. `log` alone cannot do that, because its argument is evaluated
		before it checks ENABLED.
	**/
	public static inline function isEnabled():Bool {
		return ENABLED;
	}

	// Several threads trace, and unsynchronized stderr writes interleave their bytes.
	static final lock = new sys.thread.Mutex();

	public static function log(message:String):Void {
		if (!ENABLED) {
			return;
		}
		lock.acquire();
		try {
			Sys.stderr().writeString(message + "\n");
			Sys.stderr().flush();
		} catch (e:Dynamic) {}
		lock.release();
	}
}
