package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.v2.testing.HaxeTestNameLocation;
import com.intellij.util.PathUtil;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The stdout-TeamCity converter for Haxe test runs. Two event shapes arrive on
 * the same stream: the injected live reporter's per-test events during the run
 * (when the Build Tools setting enables the injection and it applied), and
 * utest's batch reporter replaying the whole tree at the end - completed names
 * identify the replay, which is swallowed, so either shape alone drives the
 * tree correctly. On top of that the converter papers over what the batch
 * shape lacks: a {@code haxe:test://<test name>} locationHint is injected into
 * every started event ({@link HaxeTestLocator} resolves the name to PSI); a
 * {@code testFinished} is synthesized after a {@code testFailed} not followed
 * by its own finish (utest's batch omits it and the run ends "Terminated");
 * warnings-only failures get the warning text as their message; and trace
 * lines arriving OUTSIDE any running test are attributed by position prefix
 * ({@link HaxeTestOutputAttributor}) and replayed as {@code testStdOut} inside
 * that test's started/finished pair.
 */
public class HaxeTestEventsConverter extends OutputToGeneralTestEventsConverter {

  // a whole service message: group 1 the event kind, group 2 the attribute list
  private static final Pattern EVENT_SHAPE = Pattern.compile("^##teamcity\\[(\\w+)(.*)]$");

  // one attribute inside a service message; the value is TC-escaped
  // ('|x' escapes, so a bare quote ends it)
  private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)='((?:[^'|]|\\|.)*)'");

  // an assertion-letter message: one letter per assertion, '.' success and
  // 'W' warning only - any other letter (F/E/S/T/O/A) is a real failure class
  private static final Pattern WARNING_ONLY_LETTERS = Pattern.compile("[.W]*W[.W]*");

  /**
   * One parsed service message. utest composes events from a Map, and haxe map
   * iteration order is TARGET-DEPENDENT - interp prints {@code name} first, HL
   * prints it last - so nothing may assume an attribute order.
   */
  private record TcEvent(@NotNull String kind, @NotNull Map<String, String> attributes) {
    @Nullable
    String name() {
      return attributes.get("name");
    }

    boolean is(@NotNull String expectedKind) {
      return kind.equals(expectedKind);
    }
  }

  @Nullable
  private static TcEvent parseEvent(@NotNull String trimmed) {
    Matcher event = EVENT_SHAPE.matcher(trimmed);
    if (!event.matches()) return null;
    Map<String, String> attributes = new LinkedHashMap<>();
    Matcher attribute = ATTRIBUTE.matcher(event.group(2));
    while (attribute.find()) {
      attributes.put(attribute.group(1), attribute.group(2));
    }
    return new TcEvent(event.group(1), attributes);
  }

  // utest never emits testFinished after testFailed (the TC protocol requires
  // it; without it the test dangles and the run ends "Terminated"). The LIVE
  // reporter does emit a genuine finish (with the duration) right after its
  // failure - so synthesis is DEFERRED until the next event shows whether a
  // genuine finish follows. Some event always does: both report shapes close
  // their suites after the last test.
  private String pendingFailedFinish;

  // tests and suites that already completed. The injected live reporter
  // streams every event DURING the run and utest's batch reporter then
  // replays the whole tree at the end - completed names identify the replay,
  // which is swallowed. Without injection nothing completes early and the
  // batch passes through untouched.
  private final Set<String> completedTests = new HashSet<>();
  private final Set<String> finishedSuites = new HashSet<>();
  private String runningTest;

  // trace lines attributed by their position prefix, waiting for the batch's
  // testStarted of the printing test (see HaxeTestOutputAttributor)
  private final Map<String, StringBuilder> pendingOutput = new HashMap<>();
  private final HaxeTestOutputAttributor attributor;

  // the run's tests build file; name-based location hints carry it so
  // same-named classes in sibling projects resolve to THIS build's sources
  private final @Nullable String testsBuildFilePath;

  public HaxeTestEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties consoleProperties) {
    super(testFrameworkName, consoleProperties);
    this.testsBuildFilePath = buildFilePath(consoleProperties);
    // trace position prefixes are relative to where the compile ran: the tests build file's directory
    String workDirectory = testsBuildFilePath == null ? null : PathUtil.getParentPath(testsBuildFilePath);
    this.attributor = new HaxeTestOutputAttributor(consoleProperties.getProject(), workDirectory);
  }

  @Nullable
  private static String buildFilePath(@NotNull TestConsoleProperties consoleProperties) {
    if (!(consoleProperties.getConfiguration() instanceof HaxeTestRunConfiguration configuration)) return null;
    return StringUtil.nullize(configuration.getBuildFilePath(), true);
  }

  @Override
  protected void processConsistentText(@NotNull String text, @NotNull Key<?> outputType) {
    // stdout only: compiler diagnostics arrive on stderr with the same
    // `file.hx:12:` prefix shape and must not be pinned to a test. Output
    // printed WHILE a live-reported test runs already attaches through its
    // started/finished pair - buffering applies only to the batch shape,
    // where all output precedes every event.
    boolean attributable = runningTest == null
      && ProcessOutputType.isStdout(outputType)
      && !text.contains("##teamcity[");
    if (attributable) {
      String testName = attributor.testNameFor(text.trim());
      if (testName != null) {
        pendingOutput.computeIfAbsent(testName, name -> new StringBuilder()).append(text);
      }
    }
    super.processConsistentText(text, outputType);
  }

  @Override
  protected boolean processServiceMessages(@NotNull String text,
                                           @NotNull Key<?> outputType,
                                           @NotNull ServiceMessageVisitor visitor) throws ParseException {
    TcEvent event = parseEvent(text.trim());
    if (event == null) {
      return super.processServiceMessages(text, outputType, visitor);
    }
    String normalizedName = normalize(event.name());

    if (isBatchReplay(event, normalizedName)) {
      return true;
    }

    // the deferred finish for a preceding failure, unless THIS event is the
    // genuine one (the live reporter's, carrying the real duration)
    if (pendingFailedFinish != null) {
      String failedName = pendingFailedFinish;
      pendingFailedFinish = null;
      boolean genuineFollows = event.is("testFinished") && failedName.equals(event.name());
      if (!genuineFollows) {
        super.processServiceMessages("##teamcity[testFinished name='" + failedName + "']", outputType, visitor);
        completedTests.add(normalize(failedName));
      }
    }

    // a finishing test flushes first - its output must land INSIDE the
    // started/finished pair to attach
    if (event.is("testFinished") && normalizedName != null) {
      flushPendingOutput(event.name(), outputType, visitor);
    }

    String rewritten = rewriteWarningOnlyMessage(text);
    String withHint = injectLocationHint(rewritten, testsBuildFilePath);
    boolean result = super.processServiceMessages(withHint, outputType, visitor);

    if (normalizedName != null) {
      switch (event.kind()) {
        // the batch arrives after ALL output - each test's buffered lines
        // follow its started event straight away
        case "testStarted" -> {
          runningTest = normalizedName;
          flushPendingOutput(event.name(), outputType, visitor);
        }
        case "testFinished" -> {
          completedTests.add(normalizedName);
          runningTest = null;
        }
        case "testSuiteFinished" -> finishedSuites.add(normalizedName);
        case "testFailed" -> pendingFailedFinish = event.name();
        default -> { }
      }
    }
    return result;
  }

  /** Whether the event repeats a test or suite the live reporter already completed - utest's end-of-run batch. */
  private boolean isBatchReplay(@NotNull TcEvent event, @Nullable String normalizedName) {
    if (normalizedName == null) return false;
    boolean testEvent = event.is("testStarted") || event.is("testFailed")
      || event.is("testIgnored") || event.is("testFinished");
    if (testEvent && completedTests.contains(normalizedName)) return true;
    boolean suiteEvent = event.is("testSuiteStarted") || event.is("testSuiteFinished");
    return suiteEvent && finishedSuites.contains(normalizedName);
  }

  /** utest spells default-package names with a leading dot (`.MyTest.testX`); comparisons drop it. */
  @Nullable
  private static String normalize(@Nullable String name) {
    return name == null ? null : StringUtil.trimLeading(name, '.');
  }

  private void flushPendingOutput(@NotNull String testName,
                                  @NotNull Key<?> outputType,
                                  @NotNull ServiceMessageVisitor visitor) throws ParseException {
    // the attributor's names never carry the leading dot - normalize the
    // lookup only, the emitted event must repeat the reporter's exact name
    StringBuilder buffered = pendingOutput.remove(normalize(testName));
    if (buffered == null) return;
    String event = "##teamcity[testStdOut name='" + testName + "' out='" + escapeValue(buffered.toString()) + "']";
    super.processServiceMessages(event, outputType, visitor);
  }

  // TeamCity attribute-value escaping: https://www.jetbrains.com/help/teamcity/service-messages.html
  @NotNull
  private static String escapeValue(@NotNull String value) {
    return value.replace("|", "||")
      .replace("'", "|'")
      .replace("\n", "|n")
      .replace("\r", "|r")
      .replace("[", "|[")
      .replace("]", "|]");
  }

  /**
   * utest counts a warning — typically "no assertions", e.g. a version-gated
   * test body that compiled to nothing — as a failure, and its reporter's
   * message is then just the letter code W. The vshaxe test-adapter keeps the
   * failure but records the warning TEXT as the message; the same presentation
   * applies here so both IDEs show the identical verdict. Events with real
   * failure classes in the letters pass through untouched — their details
   * already carry the full text.
   */
  @NotNull
  static String rewriteWarningOnlyMessage(@NotNull String text) {
    TcEvent event = parseEvent(text.trim());
    if (event == null || !event.is("testFailed")) return text;
    String name = event.name();
    String message = event.attributes().get("message");
    String details = event.attributes().get("details");
    boolean warningsOnly = name != null && details != null
      && message != null && WARNING_ONLY_LETTERS.matcher(message).matches();
    if (!warningsOnly) return text;

    // the details hold the warning texts, TC-escaped; drop the line-break
    // escapes and indentation so they read as one message line
    String warningText = details.replace("|n", " ")
      .replace("|r", " ")
      .trim();
    return "##teamcity[testFailed name='" + name + "' message='" + warningText + "']";
  }

  /**
   * Appends {@code locationHint='haxe:test://<name>[?build=<path>]'} to a
   * started event lacking one. The name value is already TC-escaped and is
   * reused verbatim, so the new attribute stays correctly escaped; the tests
   * build file rides along so a name shared by classes in sibling projects
   * resolves to THIS build's sources. Non-matching text passes through
   * untouched — reporter-emitted hints (buddy, tink) carry their own files.
   */
  @NotNull
  static String injectLocationHint(@NotNull String text, @Nullable String buildFilePath) {
    String trimmed = text.trim();
    TcEvent event = parseEvent(trimmed);
    boolean started = event != null && (event.is("testStarted") || event.is("testSuiteStarted"));
    if (!started || event.name() == null || event.attributes().containsKey("locationHint")) {
      return text;
    }
    String buildSuffix = buildFilePath == null ? "" : HaxeTestNameLocation.BUILD_HINT + escapeValue(buildFilePath);
    String beforeClosingBracket = trimmed.substring(0, trimmed.length() - 1);
    return beforeClosingBracket + " locationHint='" + HaxeTestNameLocation.PROTOCOL + "://" + event.name() + buildSuffix + "']";
  }
}
