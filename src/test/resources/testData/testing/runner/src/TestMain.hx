/**
 * Self-contained stand-in for a utest TeamCity run (no haxelib dependency):
 * prints the exact service-message shapes utest's TeamcityReport emits -
 * suite = `package_with_underscores.ClassName`, test = `suite.method`,
 * `testFailed` with message/details - but only when the run was activated
 * with `-D teamcity`, proving the activation define reached the compile.
 */
class TestMain {
  static function main() {
    #if teamcity
    Sys.println("");
    Sys.println("##teamcity[testSuiteStarted name='Target: Undefined']");
    Sys.println("##teamcity[testSuiteStarted name='cases.SampleTest']");
    Sys.println("##teamcity[testStarted name='cases.SampleTest.testPasses']");
    Sys.println("##teamcity[testFinished name='cases.SampleTest.testPasses']");
    Sys.println("##teamcity[testStarted name='cases.SampleTest.testFails']");
    Sys.println("##teamcity[testFailed name='cases.SampleTest.testFails' message='F' details='  line: 12, expected 2 but it is 3|n']");
    Sys.println("##teamcity[testSuiteFinished name='cases.SampleTest']");
    Sys.println("##teamcity[testSuiteFinished name='Target: Undefined']");
    #else
    Sys.println("teamcity reporting not activated");
    #end
  }
}
