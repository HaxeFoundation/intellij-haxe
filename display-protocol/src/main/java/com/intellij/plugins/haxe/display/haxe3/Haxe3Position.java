package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A position as haxe 3 prints it: {@code <file>:<line>: characters <from>-<to>}
 * for a span on one line, in byte columns, or {@code <file>:<line>: lines <from>-<to>}
 * for whole lines, which carries no columns. Lines are 1-based.
 *
 * @param wholeLines whether {@code from} and {@code to} are lines rather than byte columns on {@code line}
 */
record Haxe3Position(String file, int line, boolean wholeLines, int from, int to) {

  // the path (greedy, so the position starts at the LAST ":<line>: "; a drive letter's colon stays
  // in the path), the 1-based line, the span kind and its two numbers
  private static final Pattern POSITION =
    Pattern.compile("^(?<file>.+):(?<line>\\d+): (?<kind>characters|lines) (?<from>\\d+)-(?<to>\\d+)$");

  /** The position the text names; null when it is no position. */
  static Haxe3Position parse(String text) {
    if (text == null) return null;
    Matcher matcher = POSITION.matcher(text.strip());
    if (!matcher.matches()) return null;
    int line = Integer.parseInt(matcher.group("line"));
    boolean wholeLines = matcher.group("kind").equals("lines");
    int from = Integer.parseInt(matcher.group("from"));
    int to = Integer.parseInt(matcher.group("to"));
    return new Haxe3Position(matcher.group("file"), line, wholeLines, from, to);
  }

  /** The 0-based range in the file's text; without the text, byte columns count as characters. */
  Range rangeIn(Haxe3SourceText text) {
    if (wholeLines) {
      Position end = text != null ? text.lineEnd(to - 1) : new Position(to - 1, 0);
      return new Range(new Position(from - 1, 0), end);
    }
    int lineIndex = line - 1;
    if (text == null) return new Range(new Position(lineIndex, from), new Position(lineIndex, to));
    return new Range(text.position(lineIndex, from), text.position(lineIndex, to));
  }
}
