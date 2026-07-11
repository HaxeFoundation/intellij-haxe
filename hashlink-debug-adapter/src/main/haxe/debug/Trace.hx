package debug;

/**
 * Breadcrumbs to stderr for diagnosing hangs and lost messages; enabled by the
 * DAP_ADAPTER_TRACE environment variable. The integration tests set it and dump
 * the pipe on teardown, so a wedged run self-diagnoses: the last breadcrumb
 * printed tells you where things stopped.
 */
class Trace {
	public static final ENABLED = Sys.getEnv("DAP_ADAPTER_TRACE") != null;

	// Worker, writer and session threads all trace; unsynchronized concurrent
	// stderr writes interleave bytes and produce unreadable evidence.
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
