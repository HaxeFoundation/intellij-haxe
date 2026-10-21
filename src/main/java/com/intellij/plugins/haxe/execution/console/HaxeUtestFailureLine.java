package com.intellij.plugins.haxe.execution.console;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One utest assertion failure line: utest prints the failing assertion's
 * {@code haxe.PosInfos} as {@code src/ShapeTest.hx:174: expected 0 but it is
 * "bogus"} - path, colon, line, colon, message - and this is the file and
 * line it names plus where that span sits in the text.
 */
record HaxeUtestFailureLine(@NotNull String path, int line, int pathStart, int lineEnd) {

  // "<path>.hx:<line>: <message>" at the start of the line (a drive letter allowed in
  // the path); a compiler message's ": characters 4-9" / ": lines 4-9" tail is excluded,
  // that line belongs to HaxeCompilerMessageFilter
  private static final Pattern UTEST_FAILURE =
    Pattern.compile("^\\s*(?<path>(?:\\w:)?[^:]+?\\.hx):(?<line>\\d+):\\s+(?!(?:characters|lines)\\s+\\d+-\\d+)\\S");

  @Nullable
  static HaxeUtestFailureLine parse(@NotNull String text) {
    Matcher matcher = UTEST_FAILURE.matcher(text);
    if (!matcher.find()) return null;
    int line = Integer.parseInt(matcher.group("line"));
    return new HaxeUtestFailureLine(matcher.group("path"), line, matcher.start("path"), matcher.end("line"));
  }
}
