package com.intellij.plugins.haxe.execution.console;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One {@code Called from} line of a Haxe stack trace: the source file and
 * line it names, and where that span sits in the text. Two spellings exist:
 * the neko VM's own dump ({@code Called from shapes/Circle.hx line 86}; a
 * frame without source prints {@code Called from a C function})
 * and {@code haxe.CallStack}'s ({@code Called from Main.main (Main.hx line 12)},
 * HashLink spells it {@code Called from Main.main(Main.hx:12)}).
 */
record HaxeStackFrameLine(@NotNull String path, int line, int pathStart, int lineEnd) {

  // haxe.CallStack: optional qualified name (Haxe's closure spellings Class.~method.1
  // and fun$3 included), then "(path line N)" or "(path:N)"; path = up to the ".hx"
  private static final Pattern CALL_STACK_FRAME = Pattern.compile(
    "Called from (?:[\\w.$~]+)?\\s*"
    + "\\((?<path>[^()]+?\\.hx)(?: line |:)(?<line>\\d+)\\)",
    Pattern.CASE_INSENSITIVE);
  // neko VM: "Called from <path> line N" with no name and no parentheses;
  // the path may hold spaces and a drive letter
  private static final Pattern NEKO_FRAME =
    Pattern.compile("Called from (?<path>[^()\\s][^()]*?\\.hx) line (?<line>\\d+)", Pattern.CASE_INSENSITIVE);

  @Nullable
  static HaxeStackFrameLine parse(@NotNull String text) {
    Matcher matcher = CALL_STACK_FRAME.matcher(text);
    if (!matcher.find()) {
      matcher = NEKO_FRAME.matcher(text);
      if (!matcher.find()) return null;
    }
    int line = Integer.parseInt(matcher.group("line"));
    return new HaxeStackFrameLine(matcher.group("path"), line, matcher.start("path"), matcher.end("line"));
  }
}
