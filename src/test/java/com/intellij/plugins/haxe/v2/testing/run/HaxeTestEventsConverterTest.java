package com.intellij.plugins.haxe.v2.testing.run;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.intellij.plugins.haxe.v2.testing.run.HaxeTestEventsConverter.injectLocationHint;
import static com.intellij.plugins.haxe.v2.testing.run.HaxeTestEventsConverter.rewriteWarningOnlyMessage;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure text-level checks of the locationHint injection - the platform-parser leg
 * of the same rewrite is covered by the live pipeline test.
 */
@DisplayName("Test runner: events converter")
public class HaxeTestEventsConverterTest {

  @Test
  @DisplayName("test started gains a location hint")
  public void testTestStartedGainsALocationHint() {
    String expected = """
      ##teamcity[testStarted name='cases.SampleTest.testPasses' \
      locationHint='haxe:test://cases.SampleTest.testPasses']\
      """;
    assertEquals(expected, injectLocationHint("##teamcity[testStarted name='cases.SampleTest.testPasses']", null));
  }

  @Test
  @DisplayName("build path rides the injected hint")
  public void testBuildPathRidesTheInjectedHint() {
    // the path is TC-escaped in the attribute; the platform unescapes it on parse
    String expected = """
      ##teamcity[testStarted name='A.testX' \
      locationHint='haxe:test://A.testX?build=C:/p/tests |[x|].hxml']\
      """;
    assertEquals(expected, injectLocationHint("##teamcity[testStarted name='A.testX']", "C:/p/tests [x].hxml"));
  }

  @Test
  @DisplayName("suite started gains a location hint")
  public void testSuiteStartedGainsALocationHint() {
    String expected = """
      ##teamcity[testSuiteStarted name='cases.SampleTest' \
      locationHint='haxe:test://cases.SampleTest']\
      """;
    assertEquals(expected, injectLocationHint("##teamcity[testSuiteStarted name='cases.SampleTest']", null));
  }

  @Test
  @DisplayName("existing location hint is kept")
  public void testExistingLocationHintIsKept() {
    String withHint = "##teamcity[testStarted name='A.testX' locationHint='file://x']";
    assertEquals(withHint, injectLocationHint(withHint, null));
  }

  @Test
  @DisplayName("escaped quote in the name survives")
  public void testEscapedQuoteInTheNameSurvives() {
    String expected = """
      ##teamcity[testStarted name='A.test|'quoted|'' \
      locationHint='haxe:test://A.test|'quoted|'']\
      """;
    assertEquals(expected, injectLocationHint("##teamcity[testStarted name='A.test|'quoted|'']", null));
  }

  @Test
  @DisplayName("other events and plain output pass through")
  public void testOtherEventsAndPlainOutputPassThrough() {
    String failed = "##teamcity[testFailed name='A.testX' message='F' details='boom']";
    assertEquals(failed, injectLocationHint(failed, null));
    assertEquals("plain output line", injectLocationHint("plain output line", null));
  }

  @Test
  @DisplayName("assertion less warning failure gets the warning text as its message")
  public void testAssertionLessWarningFailureGetsTheWarningTextAsItsMessage() {
    String rewritten = rewriteWarningOnlyMessage(
      "##teamcity[testFailed name='unit.AesTest.test_ff1' message='W' details='    no assertions|n']");
    assertEquals("##teamcity[testFailed name='unit.AesTest.test_ff1' message='no assertions']", rewritten);
  }

  @Test
  @DisplayName("attribute order is target dependent and must not matter")
  public void testAttributeOrderIsTargetDependentAndMustNotMatter() {
    // utest composes events from a Map and haxe map iteration order differs
    // per target - an HL run really emits the name LAST
    assertEquals("##teamcity[testFailed name='unit_crypto.AesTest.test_ff1' message='no assertions']",
                 rewriteWarningOnlyMessage(
                   "##teamcity[testFailed message='W' details='    no assertions|n' name='unit_crypto.AesTest.test_ff1']"));
  }

  @Test
  @DisplayName("passing assertions with a warning also read as the warning text")
  public void testPassingAssertionsWithAWarningAlsoReadAsTheWarningText() {
    assertEquals("##teamcity[testFailed name='A.testX' message='check manually']",
                 rewriteWarningOnlyMessage(
                   "##teamcity[testFailed name='A.testX' message='.W.' details='    check manually|n']"));
  }

  @Test
  @DisplayName("real failures keep their event untouched")
  public void testRealFailuresKeepTheirEventUntouched() {
    String realFailure = "##teamcity[testFailed name='A.testX' message='F' details='line: 12, boom|n']";
    assertEquals(realFailure, rewriteWarningOnlyMessage(realFailure));
    String mixed = "##teamcity[testFailed name='A.testX' message='FW' details='boom|n']";
    assertEquals(mixed, rewriteWarningOnlyMessage(mixed));
  }

  @Test
  @DisplayName("warning letter inside a real message is untouched")
  public void testWarningLetterInsideARealMessageIsUntouched() {
    String realMessage = "##teamcity[testFailed name='A.testX' message='expected W' details='boom']";
    assertEquals(realMessage, rewriteWarningOnlyMessage(realMessage));
  }
}
