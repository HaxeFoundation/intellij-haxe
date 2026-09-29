package intellij_tink;

#if !macro
import intellij_haxe_test.TcOutput.announceHostedRunFinished;
import intellij_haxe_test.TcOutput.escape;
import intellij_haxe_test.TcOutput.printLine;
import tink.testrunner.Assertion;
import tink.testrunner.Reporter;
import tink.testrunner.Result;
#if flash
import intellij_haxe_test.FlashSupport;
#end

using tink.CoreApi;

/**
	tink_testrunner reporter emitting TeamCity service messages, installed as
	`Runner.run`'s default reporter by the IDE's build macro (see Macro.hx).
	Cases report as adjacent started/finished pairs from `CaseFinish` - the
	runner's result types carry no timing, so durations stay unreported.
**/
class TcReporter implements Reporter {
	final rootSuite:String;
	var rootOpen:Bool = false;

	public function new(rootSuite:String) {
		this.rootSuite = rootSuite;
		#if flash
		FlashSupport.hookTrace();
		#end
	}

	public function report(type:ReportType):Future<Noise> {
		switch type {
			case BatchStart:
				if (rootSuite != "" && !rootOpen) {
					printLine("##teamcity[testSuiteStarted name='" + escape(rootSuite) + "']");
					rootOpen = true;
				}
			case SuiteStart(info, _):
				printLine("##teamcity[testSuiteStarted name='" + escape(info.name) + "']");
			case CaseStart(_, _):
			case Assertion(_):
			case CaseFinish(result):
				reportCase(result);
			case SuiteFinish(result):
				printLine("##teamcity[testSuiteFinished name='" + escape(result.info.name) + "']");
			case BatchFinish(result):
				if (rootOpen) {
					printLine("##teamcity[testSuiteFinished name='" + escape(rootSuite) + "']");
					rootOpen = false;
				}
				announceHostedRunFinished(result.summary().failures.length > 0);
				// On flash nothing ends the process by itself. Every line
				// above is already flushed.
				#if flash
				FlashSupport.exit(0);
				#end
		}
		return Future.NOISE;
	}

	function reportCase(result:CaseResult):Void {
		switch (result.result) {
			case Excluded:
				// A case outside the run (skipped by a single-test run's
				// include mode, or marked @:exclude) is left out of the tree
				// entirely rather than shown as ignored.
				return;
			default:
		}
		var name = result.info.name;
		// The case's PosInfos has the real method name and the source FILE.
		// tink's builder drops the class's package, so the file is sent along;
		// the IDE uses it to find the class when the bare name is ambiguous or
		// the class is in a package.
		var location = result.info.pos != null ? " locationHint='haxe:tink://"
			+ escape(result.info.pos.fileName + "::" + result.info.pos.className + "." + result.info.pos.methodName)
			+ "'" : "";
		printLine("##teamcity[testStarted name='" + escape(name) + "'" + location + "]");
		switch (result.result) {
			case Succeeded(assertions):
				var failed = assertions.filter(function(a) return !a.holds);
				if (failed.length > 0) {
					var message = failed[0].description;
					var details = "";
					for (assertion in failed) {
						details += assertion.description
							+ (assertion.pos != null ? " at " + assertion.pos.fileName + ":" + assertion.pos.lineNumber : "")
							+ "\n";
					}
					printLine("##teamcity[testFailed name='" + escape(name) + "' message='" + escape(message)
						+ "' details='" + escape(details) + "']");
				}
			case Failed(e):
				printLine("##teamcity[testFailed name='" + escape(name) + "' message='" + escape(Std.string(e)) + "']");
			default: // Excluded returned above
		}
		printLine("##teamcity[testFinished name='" + escape(name) + "']");
	}
}
#end
