// A template for the entry point the IDE generates for a gutter-started
// single-test run. The suite's case whose method matches gets
// include=true, which puts the runner into include mode: every other case
// is skipped. The IDE substitutes the TEST_CLASS and TEST_METHOD tokens
// before the compile; this file is never compiled as it is.
class IjSingleRun {
	static function main() {
		var batch = tink.unit.TestBatch.make([new ${TEST_CLASS}()]);
		for (suite in batch.suites)
			for (caze in suite.cases)
				if (caze.info.pos.methodName == "${TEST_METHOD}")
					caze.include = true;
		tink.testrunner.Runner.run(batch).handle(exitHost);
	}

	// tink's own Runner.exit throws "not supported" on plain js, because its
	// exit helper knows only the travix, nodejs and phantom hosts. A plain-js
	// build hosted by node reaches the exit through the process global instead.
	static function exitHost(result:tink.testrunner.Result.BatchResult):Void {
		var code:Int = result.summary().failures.length;
		#if (sys || nodejs)
		Sys.exit(code);
		#elseif js
		var proc:Dynamic = js.Syntax.code("typeof process !== 'undefined' ? process : null");
		if (proc != null) proc.exit(code);
		#else
		tink.testrunner.Runner.exit(result);
		#end
	}
}
