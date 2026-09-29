package com.intellij.plugins.haxe.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text-level documentation extraction for rendering: operates on a doc
 * comment's raw text (delimiter stripping, indent removal, blank-line
 * normalization), independent of the lazily parsed doc sub-tree.
 */
public class HaxeDocumentationUtil {


  @NotNull
  public static String unwrapCommentDelimiters(@NotNull String text) {
    if (text.startsWith("/**")) text = text.substring("/**".length());
    if (text.startsWith("/*")) text = text.substring("/*".length());
    if (text.startsWith("//")) text = text.substring("//".length());
    if (text.endsWith("**/")) text = text.substring(0, text.length() - "**/".length());
    if (text.endsWith("*/")) text = text.substring(0, text.length() - "*/".length());
    return text;
  }



  /**
   * A doc comment's body (the text between its delimiters, as the PSI or
   * the compiler hand it out) made renderable as markdown: the blank lines
   * around it and the source indentation stripped, a javadoc-style leading
   * {@code *} per line removed, and a blank line between two indented lines
   * kept at their depth so it does not open a code block.
   */
  @NotNull
  public static String stripForRendering(@NotNull String docBody) {
    String trimmed = removeExcessLines(docBody);
    String stripped = stripIndents(trimmed, docIsJavadocStyle(trimmed));
    return tryFixIndents(stripped);
  }

  public static String removeExcessLines(String docs) {
    String[] split = docs.split("\n");
    if (split.length == 1)  return docs;
    // multi-line docs will contain the empty lines after /** and before */
    List<String> fragments = new ArrayList<>(List.of(split));

    while (!fragments.isEmpty() && fragments.getFirst().isBlank()) {
      fragments.removeFirst();
    }
    while (!fragments.isEmpty() && fragments.getLast().isBlank()) {
      fragments.removeLast();
    }

    return String.join("\n", fragments);
  }

  public static boolean docIsJavadocStyle(String rawText) {
    String[] split1 = rawText.split("\n");
    return Arrays.stream(split1).allMatch(str -> str.matches("^\\s*\\*.*"));
  }

  public static String stripIndents(String docs, boolean javaDocStyle) {
    String[] split = docs.split("\n");
    if(javaDocStyle) {
      return Arrays.stream(split).map(s-> s.replaceFirst("\\s*\\*","")).collect(Collectors.joining("\n"));
    }
    // the RENDERING indent rule: shortest leading whitespace among lines with
    // content - hard-wrapped column-0 lines must not drag it to nothing, and
    // blank lines (even indented ones) never count, or surviving indentation
    // renders every paragraph as a markdown code block. Deliberately stricter
    // than HaxeDocLexer.commonPrefix, whose reference-formatter mirror counts
    // whitespace-only interior lines.
    String prefix = null;
    for (String line : split) {
      if (line.isBlank()) continue;
      String leading = leadingWhitespace(line);
      if (leading.isEmpty()) continue;
      if (prefix == null || leading.length() < prefix.length()) {
        prefix = leading;
      }
    }
    final String strip = prefix;
    return Arrays.stream(split)
      .map(line -> stripPrefix(line, strip))
      .collect(Collectors.joining("\n"));
  }

  /** The line's leading run of spaces, tabs and carriage returns. */
  @NotNull
  public static String leadingWhitespace(@NotNull String line) {
    int i = 0;
    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t' || line.charAt(i) == '\r')) i++;
    return line.substring(0, i);
  }

  /** Blank lines strip entirely; other lines drop the common prefix when they carry it. */
  private static String stripPrefix(@NotNull String line, @Nullable String prefix) {
    if (line.isBlank()) return "";
    if (prefix != null && line.startsWith(prefix)) return line.substring(prefix.length());
    return line;
  }

  public static String tryFixIndents(String extractedDocs) {
    String[] split = extractedDocs.split("\n");
    int lineCount = split.length;
    for (int i = 0; i < lineCount; i++) {

      int lineBeforeIndex = i - 1;
      int lineAfterIndex = i + 1;

      if (lineBeforeIndex > 0) {
        if (lineAfterIndex < lineCount) {
          String line = split[i];
          String lineBefore = split[lineBeforeIndex];
          String lineAfter = split[lineAfterIndex];
          if (line.isEmpty()) {
            int beforeIndents = countIndents(lineBefore);
            int afterIndents = countIndents(lineAfter);
            if (beforeIndents == afterIndents && afterIndents != 0) {
              String indents = lineAfter.substring(0, beforeIndents);
              split[i] = indents +line+"<br>";
            }
          }
        }

      }
    }
    return String.join("\n", split);
  }

  private static int countIndents(String lineBefore) {
    return lineBefore.length() - lineBefore.stripLeading().length();
  }
}
