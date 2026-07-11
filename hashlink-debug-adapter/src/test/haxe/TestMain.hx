/**
 * Entry point of the Haxe-side adapter tests, run with `haxe test.hxml` (interpreter mode).
 * Exits non-zero when any assertion failed so the Gradle Exec task fails the build.
 *
 * To register a new suite, add ONE line to `suites` below (fully qualified,
 * no import needed). The context name shown on failures is derived from the
 * class name.
 */
class TestMain {
	static function main():Void {
		var suites:Array<{name:String, run:Assert->Void}> = [
			// transport + protocol + dispatch
			{name: "FrameCodecTest", run: tests.FrameCodecTest.run},
			{name: "ProtocolJsonTest", run: tests.ProtocolJsonTest.run},
			{name: "DispatcherTest", run: tests.DispatcherTest.run},
			// module / jit metadata
			{name: "JitInfoReaderTest", run: tests.JitInfoReaderTest.run},
			{name: "ModuleDebugInfoTest", run: tests.ModuleDebugInfoTest.run},
			{name: "CodeGraphTest", run: tests.CodeGraphTest.run},
			{name: "LocalsResolverTest", run: tests.LocalsResolverTest.run},
			// target control
			{name: "BreakpointsTest", run: tests.BreakpointsTest.run},
			{name: "StackWalkerTest", run: tests.StackWalkerTest.run},
			// memory layout
			{name: "FrameLayoutTest", run: tests.FrameLayoutTest.run},
			{name: "ObjectLayoutTest", run: tests.ObjectLayoutTest.run},
			{name: "EnumLayoutTest", run: tests.EnumLayoutTest.run},
			{name: "GlobalTableTest", run: tests.GlobalTableTest.run},
			// value decoding
			{name: "RuntimeTypesTest", run: tests.RuntimeTypesTest.run},
			{name: "ValuePathTest", run: tests.ValuePathTest.run},
			{name: "ValueReaderTest", run: tests.ValueReaderTest.run},
			{name: "ValueChildrenTest", run: tests.ValueChildrenTest.run},
			{name: "MapReaderTest", run: tests.MapReaderTest.run},
		];

		var assert = new Assert();
		for (suite in suites) {
			assert.setContext(suite.name);
			suite.run(assert);
		}
		Sys.println(assert.checks + " checks, " + assert.failures + " failures");
		Sys.exit(assert.failures == 0 ? 0 : 1);
	}
}
