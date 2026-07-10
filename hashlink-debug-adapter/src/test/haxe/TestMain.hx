import tests.DispatcherTest;
import tests.FrameCodecTest;
import tests.ProtocolJsonTest;

/**
 * Entry point of the Haxe-side adapter tests, run with `haxe test.hxml` (interpreter mode).
 * Exits non-zero when any assertion failed so the Gradle Exec task fails the build.
 */
class TestMain {
	static function main():Void {
		var assert = new Assert();

		assert.setContext("FrameCodecTest");
		FrameCodecTest.run(assert);

		assert.setContext("ProtocolJsonTest");
		ProtocolJsonTest.run(assert);

		assert.setContext("DispatcherTest");
		DispatcherTest.run(assert);

		Sys.println(assert.checks + " checks, " + assert.failures + " failures");
		Sys.exit(assert.failures == 0 ? 0 : 1);
	}
}
