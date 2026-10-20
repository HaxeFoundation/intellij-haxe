package com.intellij.plugins.haxe.v2.testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The reporting-argument contract every framework shares, as one table.
 * buddy/munit/tink have no TeamCity reporter of their own — the extracted one
 * IS the result channel, so the live-reporting toggle keeps it and a missing
 * reporter root leaves the run console-only. utest's {@code -D teamcity}
 * batch reporting stands alone: it survives both, and the toggle drops only
 * the optional live injection.
 */
@DisplayName("Test runner: framework reporting args")
public class HaxeTestFrameworkReportingArgsTest {

  private static final String SUITE = "Target: Neko";
  private static final String ROOT = "reporter-root";

  /** utest without an injection: batch reporting plus the suite label. */
  private static final List<String> UTEST_BATCH =
    List.of("-D", "teamcity", "-D", "teamcity_suite_name=" + SUITE);

  static Stream<Arguments> frameworks() {
    List<String> buddyLive = List.of("-D", "teamcity_suite_name=" + SUITE,
                                     "-D", "reporter=intellij_buddy.TcReporter",
                                     "-cp", ROOT);
    List<String> munitLive = List.of("-D", "teamcity_suite_name=" + SUITE,
                                     "-cp", ROOT,
                                     "--macro", "intellij_munit.Macro.init()");
    List<String> tinkLive = List.of("-D", "teamcity_suite_name=" + SUITE,
                                    "-cp", ROOT,
                                    "--macro", "intellij_tink.Macro.init()");
    List<String> utestLive = List.of("-D", "teamcity",
                                     "-D", "teamcity_suite_name=" + SUITE,
                                     "-cp", ROOT,
                                     "--macro", "intellij_utest.Macro.init()");
    return Stream.of(
      arguments("buddy", new BuddyFramework(), buddyLive, buddyLive, List.of()),
      arguments("munit", new MunitFramework(), munitLive, munitLive, List.of()),
      arguments("tink", new TinkFramework(), tinkLive, tinkLive, List.of()),
      arguments("utest", new UtestFramework(), utestLive, UTEST_BATCH, UTEST_BATCH));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("frameworks")
  @DisplayName("reporting args follow the reporter root and the live toggle")
  public void testReportingArgsFollowTheReporterRootAndTheLiveToggle(String name, HaxeTestFramework framework,
                                                                     List<String> live, List<String> toggledOff,
                                                                     List<String> withoutRoot) {
    assertEquals(live, framework.reportingArgs(SUITE, ROOT, true), "live reporting with the extracted reporter");
    assertEquals(toggledOff, framework.reportingArgs(SUITE, ROOT, false), "live toggle off, reporter still extracted");
    assertEquals(withoutRoot, framework.reportingArgs(SUITE, null, true), "no extracted reporter");
  }

  static Stream<Arguments> frameworksWithoutAFilterDefine() {
    return Stream.of(
      arguments("buddy", new BuddyFramework()),
      arguments("munit", new MunitFramework()),
      arguments("tink", new TinkFramework()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("frameworksWithoutAFilterDefine")
  @DisplayName("frameworks without a filter define answer no filter args")
  public void testFrameworksWithoutAFilterDefineAnswerNoFilterArgs(String name, HaxeTestFramework framework) {
    assertTrue(framework.filterArgs("Any.pattern").isEmpty(), name + " has no filter define");
  }

  @Test
  @DisplayName("utest filter args carry the pattern define")
  public void testUtestFilterArgsCarryThePatternDefine() {
    UtestFramework framework = new UtestFramework();
    assertEquals(List.of("-D", "UTEST_PATTERN=MathTest.testAddition"), framework.filterArgs("MathTest.testAddition"));
    assertTrue(framework.filterArgs(null).isEmpty(), "no pattern, no define");
  }

  @Test
  @DisplayName("utest batch reporting survives with nothing extracted")
  public void testUtestBatchReportingSurvivesWithNothingExtracted() {
    List<String> floor = new UtestFramework().reportingArgs(null, null, false);
    assertEquals(List.of("-D", "teamcity"), floor, "the batch define alone, no suite label without a name");
  }
}
