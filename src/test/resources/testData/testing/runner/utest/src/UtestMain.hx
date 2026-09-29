import utest.Assert;
import utest.Runner;
import utest.ui.Report;

/**
 * Real-utest fixture for the live reporter: `Report.create` under
 * `-D teamcity` instantiates the shadow TeamcityReport from the classpath
 * the planner appends. One test traces (output attribution), one passes
 * silently, one has no assertions (warnings-only mapping).
 */
class UtestMain {
	static function main() {
		var runner = new Runner();
		runner.addCase(new LiveCase());
		Report.create(runner);
		runner.run();
	}
}

class LiveCase implements utest.ITest {
	public function new() {}

	public function testTraces() {
		trace("hello from the traced test");
		Assert.isTrue(true);
	}

	public function testPasses() {
		Assert.equals(2, 1 + 1);
	}

	public function testNoAsserts() {}
}
