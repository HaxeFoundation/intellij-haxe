// A template for the entry point the IDE generates for a gutter-started
// run: a munit TestSuite holding just the selected test classes, compiled
// with the tests build's classpaths, defines and libraries. The live client
// attaches through the intellij_munit macro, as in a full run. The IDE
// substitutes the ADD_SUITES token (one add() per selected class) before
// the compile; this file is never compiled as it is. Comments name the
// tokens without their ${} wrapper: the substitution also runs over comments,
// and a multi-line value would push text out of the comment.
//
// No completionHandler is set on purpose. On sys targets munit runs on a
// worker thread, and its handler dispatch calls haxe.Timer.delay there. On
// HashLink that thread has no event loop, so a handler crashes the run's
// completion. Without one, the runner's thread handshake ends the process,
// and the verdicts come from the reported events.
class IjSingleSuite extends massive.munit.TestSuite {
	public function new() {
		super();
		${ADD_SUITES}
	}
}

class IjSingleRun {
	static function main() {
		// On flash, munit's PrintClient talks to the browser through
		// ExternalInterface and throws under the IDE's adl host; the summary
		// client needs no such channel.
		var client:massive.munit.ITestResultClient =
			#if flash new massive.munit.client.SummaryReportClient() #else new massive.munit.client.PrintClient() #end;
		var runner = new massive.munit.TestRunner(client);
		runner.run([IjSingleSuite]);
	}
}
