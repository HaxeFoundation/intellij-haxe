import tests.BreakpointsTest;
import tests.DispatcherTest;
import tests.FrameCodecTest;
import tests.JitInfoReaderTest;
import tests.ModuleDebugInfoTest;
import tests.ProtocolJsonTest;
import tests.StackWalkerTest;

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

		assert.setContext("JitInfoReaderTest");
		JitInfoReaderTest.run(assert);

		assert.setContext("ModuleDebugInfoTest");
		ModuleDebugInfoTest.run(assert);

		assert.setContext("BreakpointsTest");
		BreakpointsTest.run(assert);

		assert.setContext("StackWalkerTest");
		StackWalkerTest.run(assert);

		Sys.println(assert.checks + " checks, " + assert.failures + " failures");
		Sys.exit(assert.failures == 0 ? 0 : 1);
	}
}
