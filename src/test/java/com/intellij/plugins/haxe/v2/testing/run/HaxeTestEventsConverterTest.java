package com.intellij.plugins.haxe.v2.testing.run;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static com.intellij.plugins.haxe.v2.testing.run.HaxeTestEventsConverter.injectLocationHint;
import static com.intellij.plugins.haxe.v2.testing.run.HaxeTestEventsConverter.rewriteWarningOnlyMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pure text-level checks of the locationHint injection - the platform-parser leg
 * of the same rewrite is covered by the live pipeline test.
 */
@DisplayName("Test runner: events converter")
public class HaxeTestEventsConverterTest {

  /** (output line, build path, the line with its location hint). */
  static final List<Arguments> LOCATION_HINTS = List.of(
    // test started gains a location hint
    arguments("##teamcity[testStarted name='cases.SampleTest.testPasses']", null, """
      ##teamcity[testStarted name='cases.SampleTest.testPasses' \
      locationHint='haxe:test://cases.SampleTest.testPasses']\
      """),
    // build path rides the injected hint: TC-escaped in the attribute, the platform unescapes it on parse
    arguments("##teamcity[testStarted name='A.testX']", "C:/p/tests [x].hxml", """
      ##teamcity[testStarted name='A.testX' \
      locationHint='haxe:test://A.testX?build=C:/p/tests |[x|].hxml']\
      """),
    // suite started gains a location hint
    arguments("##teamcity[testSuiteStarted name='cases.SampleTest']", null, """
      ##teamcity[testSuiteStarted name='cases.SampleTest' \
      locationHint='haxe:test://cases.SampleTest']\
      """),
    // existing location hint is kept
    arguments("##teamcity[testStarted name='A.testX' locationHint='file://x']", null,
              "##teamcity[testStarted name='A.testX' locationHint='file://x']"),
    // escaped quote in the name survives
    arguments("##teamcity[testStarted name='A.test|'quoted|'']", null, """
      ##teamcity[testStarted name='A.test|'quoted|'' \
      locationHint='haxe:test://A.test|'quoted|'']\
      """),
    // other events and plain output pass through
    arguments("##teamcity[testFailed name='A.testX' message='F' details='boom']", null,
              "##teamcity[testFailed name='A.testX' message='F' details='boom']"),
    arguments("plain output line", null, "plain output line"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LOCATION_HINTS")
  @DisplayName("location hint injection")
  public void testLocationHintInjection(String line, String buildPath, String expected) {
    assertEquals(expected, injectLocationHint(line, buildPath));
  }

  /** (testFailed line, its rewrite). */
  static final List<Arguments> WARNING_REWRITES = List.of(
    // assertion less warning failure gets the warning text as its message
    arguments("##teamcity[testFailed name='unit.AesTest.test_ff1' message='W' details='    no assertions|n']",
              "##teamcity[testFailed name='unit.AesTest.test_ff1' message='no assertions']"),
    // attribute order is target dependent and must not matter: utest composes events from a Map and
    // haxe map iteration order differs per target - an HL run really emits the name LAST
    arguments("##teamcity[testFailed message='W' details='    no assertions|n' name='unit_crypto.AesTest.test_ff1']",
              "##teamcity[testFailed name='unit_crypto.AesTest.test_ff1' message='no assertions']"),
    // passing assertions with a warning also read as the warning text
    arguments("##teamcity[testFailed name='A.testX' message='.W.' details='    check manually|n']",
              "##teamcity[testFailed name='A.testX' message='check manually']"),
    // real failures keep their event untouched
    arguments("##teamcity[testFailed name='A.testX' message='F' details='line: 12, boom|n']",
              "##teamcity[testFailed name='A.testX' message='F' details='line: 12, boom|n']"),
    arguments("##teamcity[testFailed name='A.testX' message='FW' details='boom|n']",
              "##teamcity[testFailed name='A.testX' message='FW' details='boom|n']"),
    // warning letter inside a real message is untouched
    arguments("##teamcity[testFailed name='A.testX' message='expected W' details='boom']",
              "##teamcity[testFailed name='A.testX' message='expected W' details='boom']"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("WARNING_REWRITES")
  @DisplayName("warning only message rewrite")
  public void testWarningOnlyMessageRewrite(String line, String expected) {
    assertEquals(expected, rewriteWarningOnlyMessage(line));
  }
}
