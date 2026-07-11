import tests.BreakpointsTest;
import tests.CodeGraphTest;
import tests.DispatcherTest;
import tests.FrameCodecTest;
import tests.EnumLayoutTest;
import tests.FrameLayoutTest;
import tests.GlobalTableTest;
import tests.LocalsResolverTest;
import tests.ObjectLayoutTest;
import tests.RuntimeTypesTest;
import tests.ValueChildrenTest;
import tests.ValueReaderTest;
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

		assert.setContext("CodeGraphTest");
		CodeGraphTest.run(assert);

		assert.setContext("FrameLayoutTest");
		FrameLayoutTest.run(assert);

		assert.setContext("LocalsResolverTest");
		LocalsResolverTest.run(assert);

		assert.setContext("ObjectLayoutTest");
		ObjectLayoutTest.run(assert);

		assert.setContext("GlobalTableTest");
		GlobalTableTest.run(assert);

		assert.setContext("RuntimeTypesTest");
		RuntimeTypesTest.run(assert);

		assert.setContext("EnumLayoutTest");
		EnumLayoutTest.run(assert);

		assert.setContext("ValueReaderTest");
		ValueReaderTest.run(assert);

		assert.setContext("ValueChildrenTest");
		ValueChildrenTest.run(assert);

		Sys.println(assert.checks + " checks, " + assert.failures + " failures");
		Sys.exit(assert.failures == 0 ? 0 : 1);
	}
}
