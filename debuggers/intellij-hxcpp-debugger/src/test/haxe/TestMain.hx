/**
	Unit-test entry point, run under the Haxe interpreter (`haxe test.hxml`).
	Register every test class here.
**/
class TestMain {
	static function main():Void {
		var assert = new Assert();
		var tests:Array<{name:String, run:Assert->Void}> = [
			{name: "ConfigTest", run: tests.ConfigTest.run},
			{name: "FramingTest", run: tests.FramingTest.run},
			{name: "DispatcherTest", run: tests.DispatcherTest.run},
			{name: "FileMatcherTest", run: tests.FileMatcherTest.run},
			{name: "LineTableTest", run: tests.LineTableTest.run},
			{name: "BreakpointsTest", run: tests.BreakpointsTest.run},
			{name: "ValuesTest", run: tests.ValuesTest.run},
			{name: "VariablesViewTest", run: tests.VariablesViewTest.run},
			{name: "EvaluatorTest", run: tests.EvaluatorTest.run}
		];
		for (test in tests) {
			test.run(assert);
		}
		Sys.println(assert.checks + " checks, " + assert.failures + " failures");
		Sys.exit(assert.failures == 0 ? 0 : 1);
	}
}
