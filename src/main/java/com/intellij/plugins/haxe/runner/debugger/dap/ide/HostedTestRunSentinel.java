package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The completion line an IDE-injected reporter prints when a BROWSER-hosted
 * test run finishes — a page has no process exit to signal it. The run host
 * ends the run on it; the debug session strips it from the replayed output.
 */
public final class HostedTestRunSentinel {

  // the reporters' completion line: ##intellij-haxe[testRunFinished exit='0'],
  // group 1 the exit code; an optional line break is consumed with it
  private static final Pattern LINE = Pattern.compile("##intellij-haxe\\[testRunFinished exit='(\\d+)']\\r?\\n?");

  private HostedTestRunSentinel() {
  }

  /** The sentinel's exit code when the text contains one; null otherwise. */
  @Nullable
  public static Integer exitCode(@NotNull String text) {
    Matcher matcher = LINE.matcher(text);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
  }

  /** The text with any sentinel line removed (it is control flow, not test output). */
  @NotNull
  public static String strip(@NotNull String text) {
    return LINE.matcher(text).replaceAll("");
  }
}
