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
			{name: "FrameCodecTest", run: tests.dap.transport.FrameCodecTest.run},
			{name: "ProtocolJsonTest", run: tests.dap.protocol.ProtocolJsonTest.run},
			{name: "DispatcherTest", run: tests.adapter.DispatcherTest.run},
			// module / jit metadata
			{name: "JitInfoReaderTest", run: tests.debug.module.JitInfoReaderTest.run},
			{name: "ModuleDebugInfoTest", run: tests.debug.module.ModuleDebugInfoTest.run},
			{name: "CodeGraphTest", run: tests.debug.module.CodeGraphTest.run},
			{name: "LocalScopesTest", run: tests.debug.module.LocalScopesTest.run},
			{name: "LocalsResolverTest", run: tests.debug.module.LocalsResolverTest.run},
			// target control
			{name: "BreakpointsTest", run: tests.debug.session.BreakpointsTest.run},
			{name: "StackWalkerTest", run: tests.debug.target.StackWalkerTest.run},
			{name: "ThreadRegistryTest", run: tests.debug.target.ThreadRegistryTest.run},
			// memory layout
			{name: "FrameLayoutTest", run: tests.debug.layout.FrameLayoutTest.run},
			{name: "ObjectLayoutTest", run: tests.debug.layout.ObjectLayoutTest.run},
			{name: "EnumLayoutTest", run: tests.debug.layout.EnumLayoutTest.run},
			{name: "GlobalTableTest", run: tests.debug.layout.GlobalTableTest.run},
			// value decoding
			{name: "RuntimeTypesTest", run: tests.debug.values.RuntimeTypesTest.run},
			{name: "ValuePathTest", run: tests.debug.values.ValuePathTest.run},
			{name: "ValueReaderTest", run: tests.debug.values.ValueReaderTest.run},
			{name: "ValueChildrenTest", run: tests.debug.values.ValueChildrenTest.run},
			{name: "MapReaderTest", run: tests.debug.values.MapReaderTest.run},
			// expression evaluator + eval-call
			{name: "ExprParserTest", run: tests.debug.eval.ExprParserTest.run},
			{name: "OperatorsTest", run: tests.debug.eval.OperatorsTest.run},
			{name: "CallEmitterTest", run: tests.debug.eval.call.CallEmitterTest.run},
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
