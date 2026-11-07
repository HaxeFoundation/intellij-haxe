package com.intellij.plugins.haxe.ide.documentation;

import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.ide.HaxeCommenter.DOC_COMMENT_LINE_PREFIX;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.DOC_COMMENT_PREFIX;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.HAXE_DOC_COMMENT_SUFFIX;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.JAVADOC_CLOSER_LINE;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.JAVADOC_COMMENT_SUFFIX;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.JAVADOC_LINE_START;
import static com.intellij.plugins.haxe.util.HaxeDocumentationUtil.leadingWhitespace;

/// Rewrites a doc comment's text in the other of its two styles, keeping every line's text:
///
/// ```haxe
/// /**                      /**
///     Haxe style.     <->   * Javadoc style.
/// **/                       */
/// ```
///
/// The opener's line stays as written. A body line keeps its indentation relative to the other body lines; a one-line
/// comment only changes its closer.
public final class HaxeDocCommentStyleConverter {

  // a Javadoc-style line's star and the one space after it; further spaces belong to the text
  private static final Pattern STAR_LINE_START =
    Pattern.compile("^[ \\t]*" + Pattern.quote(DOC_COMMENT_LINE_PREFIX) + " ?(?<text>.*)$");

  private HaxeDocCommentStyleConverter() {
  }

  /** Whether the comment is Javadoc style; a comment without lines of its own goes by its closer. */
  public static boolean isJavadocStyle(@NotNull HaxePsiDocCommentImpl docComment) {
    if (docComment.hasInteriorLines()) return docComment.isStarStyle();
    return !docComment.getText().endsWith(HAXE_DOC_COMMENT_SUFFIX);
  }

  /** Whether the text is long enough to tell its opener from its closer; in a five-character comment they overlap. */
  public static boolean isConvertible(@NotNull HaxePsiDocCommentImpl docComment) {
    String text = docComment.getText();
    return text.startsWith(DOC_COMMENT_PREFIX) && text.length() >= DOC_COMMENT_PREFIX.length() + HAXE_DOC_COMMENT_SUFFIX.length();
  }

  /**
   * The comment in Haxe style, its body lines `bodyIndent` inside the comment.
   *
   * @param indent the indentation of the opener's line, which the closer and the body lines start with
   */
  public static @NotNull String toHaxeStyle(@NotNull String text, @NotNull String indent, @NotNull String bodyIndent) {
    if (isOneLine(text)) return withoutCloser(text) + HAXE_DOC_COMMENT_SUFFIX;

    StringBuilder converted = new StringBuilder(openerLine(text));
    for (String line : bodyLines(text, true)) {
      converted.append('\n');
      if (!line.isEmpty()) converted.append(indent).append(bodyIndent).append(line);
    }
    return converted.append('\n').append(indent).append(HAXE_DOC_COMMENT_SUFFIX).toString();
  }

  /**
   * The comment in Javadoc style.
   *
   * @param indent the indentation of the opener's line, which the closer and the body lines start with
   */
  public static @NotNull String toJavadocStyle(@NotNull String text, @NotNull String indent) {
    if (isOneLine(text)) return withoutCloser(text) + JAVADOC_COMMENT_SUFFIX;

    StringBuilder converted = new StringBuilder(openerLine(text));
    for (String line : bodyLines(text, false)) {
      converted.append('\n').append(indent);
      converted.append(line.isEmpty() ? " " + DOC_COMMENT_LINE_PREFIX : JAVADOC_LINE_START + line);
    }
    return converted.append('\n').append(indent).append(JAVADOC_CLOSER_LINE).toString();
  }

  private static boolean isOneLine(@NotNull String text) {
    return text.indexOf('\n') < 0;
  }

  private static @NotNull String openerLine(@NotNull String text) {
    return text.substring(0, text.indexOf('\n')).stripTrailing();
  }

  private static @NotNull String withoutCloser(@NotNull String text) {
    String closer = text.endsWith(HAXE_DOC_COMMENT_SUFFIX) ? HAXE_DOC_COMMENT_SUFFIX : JAVADOC_COMMENT_SUFFIX;
    return text.endsWith(closer) ? text.substring(0, text.length() - closer.length()) : text;
  }

  /**
   * The text of the lines below the opener's, without trailing whitespace and without what the source style puts in
   * front of it: a star, or the indentation the body lines share. Text in front of the closer becomes a last line.
   */
  private static @NotNull List<String> bodyLines(@NotNull String text, boolean fromJavadocStyle) {
    String[] lines = withoutCloser(text).split("\n", -1);
    List<String> body = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].stripTrailing();
      if (fromJavadocStyle) line = withoutStar(line);
      boolean closerLine = i == lines.length - 1;
      if (!closerLine || !line.isBlank()) body.add(line);
    }
    return fromJavadocStyle ? body : withoutSharedIndent(body);
  }

  private static @NotNull String withoutStar(@NotNull String line) {
    Matcher matcher = STAR_LINE_START.matcher(line);
    return matcher.matches() ? matcher.group("text") : line.strip();
  }

  /** Lines with text lose the shortest indentation among them; blank lines become empty. */
  private static @NotNull List<String> withoutSharedIndent(@NotNull List<String> lines) {
    int shared = Integer.MAX_VALUE;
    for (String line : lines) {
      if (!line.isBlank()) shared = Math.min(shared, leadingWhitespace(line).length());
    }
    List<String> stripped = new ArrayList<>();
    for (String line : lines) {
      stripped.add(line.isBlank() ? "" : line.substring(shared));
    }
    return stripped;
  }
}
