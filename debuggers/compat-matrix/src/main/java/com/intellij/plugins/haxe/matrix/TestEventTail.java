package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tails a child gradle build's console log for the per-test events enabled
 * by test-events.init.gradle ("com.x.FooTest > barTest PASSED") and turns
 * them into live progress: a throttled running total while the suite runs, an
 * immediate line for every failing test, and a per-CLASS breakdown at the end.
 * Byte-offset based so each poll only reads what is new; a trailing partial
 * line waits for the next poll.
 *
 * Counts are AGGREGATED per class in a map, not tracked as a single "current"
 * class: with -PdapTestForks the test classes run in parallel fork JVMs, so
 * their events interleave — the old single-class tracker flushed "X: 1 tests"
 * on every switch between them.
 */
final class TestEventTail {
  private static final Pattern EVENT =
    Pattern.compile("^(\\S+) > (\\S+).* (PASSED|FAILED|SKIPPED)\\s*$");
  // tests between running-total progress lines (keeps long runs alive without
  // a line per test / per fork-interleave)
  private static final int PROGRESS_EVERY = 20;

  private final Path logFile;
  private final Log log;
  private long offset;
  private String carry = "";
  // class short-name -> {tests, failed, skipped}, accumulated for the whole run
  private final Map<String, int[]> classes = new LinkedHashMap<>();
  private int total;
  private int reportedTotal;

  TestEventTail(Path logFile, Log log) {
    this.logFile = logFile;
    this.log = log;
  }

  void poll() {
    if (!Files.isRegularFile(logFile)) {
      return;
    }
    try (RandomAccessFile file = new RandomAccessFile(logFile.toFile(), "r")) {
      long length = file.length();
      if (length <= offset) {
        return;
      }
      file.seek(offset);
      byte[] chunk = new byte[(int)Math.min(length - offset, 1 << 20)];
      int read = file.read(chunk);
      offset += Math.max(read, 0);
      String text = carry + new String(chunk, 0, Math.max(read, 0), StandardCharsets.UTF_8);
      int lastNewline = text.lastIndexOf('\n');
      if (lastNewline < 0) {
        carry = text;
        return;
      }
      carry = text.substring(lastNewline + 1);
      for (String line : text.substring(0, lastNewline).split("\r?\n")) {
        handle(line);
      }
      if (total - reportedTotal >= PROGRESS_EVERY) {
        logRunningTotal();
      }
    } catch (IOException ignored) {
      // the log file being briefly unavailable only delays progress lines
    }
  }

  private void handle(String line) {
    Matcher matcher = EVENT.matcher(line);
    if (!matcher.matches()) {
      return;
    }
    String suite = matcher.group(1);
    String shortName = suite.substring(suite.lastIndexOf('.') + 1);
    int[] counts = classes.computeIfAbsent(shortName, k -> new int[3]);
    counts[0]++;
    total++;
    switch (matcher.group(3)) {
      case "FAILED" -> {
        counts[1]++;
        log.line("      FAILED " + shortName + "::" + matcher.group(2));
      }
      case "SKIPPED" -> counts[2]++;
      default -> { }
    }
  }

  private int totalFailed() {
    int failed = 0;
    for (int[] counts : classes.values()) {
      failed += counts[1];
    }
    return failed;
  }

  /** A single aggregate line so a long run shows it is alive without spam. */
  private void logRunningTotal() {
    int failed = totalFailed();
    log.line("      progress: " + total + " tests in " + classes.size() + " classes"
             + (failed > 0 ? ", " + failed + " FAILED" : ""));
    reportedTotal = total;
  }

  /** The per-class breakdown, logged once the build ends. */
  void flush() {
    if (classes.isEmpty()) {
      return;
    }
    classes.keySet().stream().sorted().forEach(name -> {
      int[] counts = classes.get(name);
      log.line("      " + name + ": " + counts[0] + " tests"
               + (counts[1] > 0 ? ", " + counts[1] + " FAILED" : "")
               + (counts[2] > 0 ? ", " + counts[2] + " skipped" : ""));
    });
    classes.clear();
    total = 0;
    reportedTotal = 0;
  }
}
