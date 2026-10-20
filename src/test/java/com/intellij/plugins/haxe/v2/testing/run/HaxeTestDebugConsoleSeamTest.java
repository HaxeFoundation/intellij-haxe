package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapTestConsoles;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The debugged-test console seam, replayed: a debugged HL test session's
 * console is built by {@code DapTestConsoles} from the test configuration's SM
 * properties and attached to the debuggee's process handler, in the same
 * order the XDebugger flow uses (attach, then startNotify, then output, then
 * termination). The debuggee's stdout is replayed from the utest TeamCity
 * reporter's recorded shape — one batch after the run, trace position prefix,
 * a service message split across two chunks — and must come out as a parsed
 * SM test tree with pass/fail states and injected navigation locations.
 */
@DisplayName("Test runner: debugged console seam")
public class HaxeTestDebugConsoleSeamTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  // the utest TeamcityReport batch as an HL debuggee prints it: trace prefix
  // line, root suite "Target: Undefined" (the reporter's target chain has no
  // hl case), and the failed event with name LAST - attribute order follows
  // haxe's target-dependent map iteration, and HL genuinely differs from interp
  private static final String RECORDED_STDOUT = """
    TestMain.hx:10:\s
    ##teamcity[testSuiteStarted name='Target: Undefined']
    ##teamcity[testSuiteStarted name='cases.SampleTest']
    ##teamcity[testStarted name='cases.SampleTest.testPasses']
    ##teamcity[testFinished name='cases.SampleTest.testPasses']
    ##teamcity[testStarted name='cases.SampleTest.testFails']
    ##teamcity[testFailed name='cases.SampleTest.testFails' message='expected true but it is false' details='cases/SampleTest.hx:12']
    ##teamcity[testStarted name='cases.SampleTest.testNoAsserts']
    ##teamcity[testFailed message='W' details='    no assertions|n' name='cases.SampleTest.testNoAsserts']
    ##teamcity[testSuiteFinished name='cases.SampleTest']
    ##teamcity[testSuiteFinished name='Target: Undefined']
    """;

  // split INSIDE a service message: the OS pipe gives no line guarantees, and
  // the console's splitter must reassemble before parsing
  private static final int SPLIT_AT = RECORDED_STDOUT.indexOf("name='cases.SampleTest.testFails' message");

  @Test
  @DisplayName("replayed debuggee stdout drives the SM test tree")
  public void testReplayedDebuggeeStdoutDrivesTheSmTestTree() {
    HaxeTestRunConfiguration configuration = newConfiguration();
    NopProcessHandler handler = new NopProcessHandler();

    ExecutionConsole console = DapTestConsoles.createTestConsole(
      configuration, DefaultDebugExecutor.getDebugExecutorInstance(), handler);
    assertNotNull(console, "a test run configuration must yield an SM console");
    SMTRunnerConsoleView smConsole = assertInstanceOf(SMTRunnerConsoleView.class, console);
    try {
      handler.startNotify();
      handler.notifyTextAvailable(RECORDED_STDOUT.substring(0, SPLIT_AT), ProcessOutputTypes.STDOUT);
      handler.notifyTextAvailable(RECORDED_STDOUT.substring(SPLIT_AT), ProcessOutputTypes.STDOUT);
      handler.destroyProcess(); // the debug session's teardown destroys the handler the same way
      UIUtil.dispatchAllInvocationEvents();

      SMTestProxy targetSuite = singleChild(smConsole.getResultsViewer().getTestsRootNode(), "Target: Undefined");
      SMTestProxy classSuite = singleChild(targetSuite, "cases.SampleTest");
      List<? extends SMTestProxy> tests = classSuite.getChildren();
      assertEquals(3, tests.size(), "all three tests must be in the tree");

      SMTestProxy passed = tests.get(0);
      assertEquals("cases.SampleTest.testPasses", passed.getName());
      assertTrue(passed.isPassed(), "testPasses must be green");

      SMTestProxy failed = tests.get(1);
      assertEquals("cases.SampleTest.testFails", failed.getName());
      assertTrue(failed.isDefect(), "testFails must be a defect");
      // the recorded batch has NO testFinished after the failure (utest never
      // emits one) - the converter's synthetic finish must close the test, or
      // the whole run ends as "Terminated" with a dangling in-progress node
      assertFalse(failed.isInProgress(), "the failed test must be closed by the synthesized testFinished");
      assertEquals("haxe:test://cases.SampleTest.testFails", failed.getLocationUrl(),
                   "the converter must have injected the navigation location");

      // the assertion-less test stays FAILED (matching the vshaxe adapter's
      // Warning -> Failure mapping) but with the warning text as its message
      // instead of utest's bare letter code W
      SMTestProxy noAsserts = tests.get(2);
      assertEquals("cases.SampleTest.testNoAsserts", noAsserts.getName());
      assertTrue(noAsserts.isDefect(), "a warnings-only test fails, as it does in the VSCode adapter");
      assertFalse(noAsserts.isIgnored(), "failed, not ignored - the VSCode adapter shows it red");
      assertFalse(noAsserts.isInProgress(), "the failed test must be closed by the synthesized testFinished");
      assertEquals("no assertions", noAsserts.getErrorMessage(),
                   "the warning text replaces the letter code as the failure message");
    } finally {
      Disposer.dispose(console);
    }
  }

  @NotNull
  private HaxeTestRunConfiguration newConfiguration() {
    HaxeTestRunConfigurationType type = new HaxeTestRunConfigurationType();
    return new HaxeTestRunConfiguration(getProject(), type.getFactory(), "debug seam");
  }

  @NotNull
  private static SMTestProxy singleChild(@NotNull SMTestProxy parent, @NotNull String expectedName) {
    List<? extends SMTestProxy> children = parent.getChildren();
    assertEquals(1, children.size(), "expected exactly '" + expectedName + "' under " + parent.getName());
    SMTestProxy child = children.get(0);
    assertEquals(expectedName, child.getName());
    return child;
  }
}
