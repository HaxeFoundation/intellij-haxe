// A template for the entry point the IDE generates for a gutter-started
// run: the selected tink_unittest suite classes of the tests build, compiled
// with that build's classpaths, defines and libraries. The IDE substitutes
// the NEW_SUITES token (one `new Suite()` per selected class) before the
// compile; this file is never compiled as it is.
class IjSingleRun {
	static function main() {
		var batch = tink.unit.TestBatch.make([${NEW_SUITES}]);
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
