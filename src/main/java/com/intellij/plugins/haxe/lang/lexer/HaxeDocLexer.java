package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes.*;

/**
 * Splits one doc comment token ({@code /** ... *}{@code /}) into line-oriented
 * sub-tokens. Haxe docs come in two styles and the whole comment is classified
 * first, mirroring haxe-formatter: JavaDoc style when EVERY interior line
 * starts with {@code *} (the star becomes DOC_LEADING_ASTERISK and its
 * whitespace is fully managed), haxedoc style otherwise (indentation only).
 *
 * For haxedoc style the COMMON leading-whitespace prefix of the interior lines
 * becomes the managed WHITE_SPACE; any deeper, author-chosen indentation stays
 * glued to the DOC_DATA token. Doc text is markdown, where relative depth is
 * meaning (nested lists, code blocks), so only the shared base may ever be
 * rewritten by the formatter - and trailing spaces (hard line breaks) belong
 * to DOC_DATA, out of the formatter's reach.
 */
public class HaxeDocLexer extends LexerBase {

  // a degenerate no-content comment: /**/ , /***/ , /*****/
  private static final Pattern DEGENERATE_DOC = Pattern.compile("/\\*\\*+/");
  // a doc tag opening a line's content: @param, @return, @see...
  private static final Pattern TAG_START = Pattern.compile("@[a-zA-Z]+");
  // the reference formatter's JavaDoc-style line test: optional indent, a star, then a break or space
  private static final Pattern STAR_LINE = Pattern.compile("^\\s*\\*(\\s.*|)$");

  private record Segment(IElementType type, int start, int end) {
  }

  private CharSequence buffer;
  private int bufferEnd;
  private List<Segment> segments;
  private int index;

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    this.buffer = buffer;
    this.bufferEnd = endOffset;
    this.segments = segment(buffer, startOffset, endOffset);
    this.index = 0;
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public @Nullable IElementType getTokenType() {
    return index < segments.size() ? segments.get(index).type : null;
  }

  @Override
  public int getTokenStart() {
    return segments.get(index).start;
  }

  @Override
  public int getTokenEnd() {
    return segments.get(index).end;
  }

  @Override
  public void advance() {
    index++;
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getBufferEnd() {
    return bufferEnd;
  }

  @NotNull
  private static List<Segment> segment(@NotNull CharSequence buffer, int start, int end) {
    List<Segment> segments = new ArrayList<>();
    String text = buffer.subSequence(start, end).toString();
    if (DEGENERATE_DOC.matcher(text).matches()) {
      segments.add(new Segment(DOC_START, start, end));
      return segments;
    }

    int contentStart = start + 2;
    while (contentStart < end && buffer.charAt(contentStart) == '*') contentStart++;
    segments.add(new Segment(DOC_START, start, contentStart));

    int contentEnd = closerStart(buffer, contentStart, end);

    String interior = buffer.subSequence(contentStart, contentEnd).toString();
    String[] lines = interior.split("\n", -1);
    boolean starStyle = isStarStyle(lines);
    String commonPrefix = starStyle ? null : commonPrefix(lines);

    int offset = contentStart;
    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      String line = lines[lineIndex];
      boolean firstLine = lineIndex == 0;
      segmentLine(segments, buffer, offset, line, firstLine, starStyle, commonPrefix);
      offset += line.length();
      if (lineIndex < lines.length - 1) {
        addOrMergeWhitespace(segments, offset, offset + 1);
        offset += 1;
      }
    }

    if (contentEnd < end) {
      segments.add(new Segment(DOC_END, contentEnd, end));
    }
    return segments;
  }

  /** The offset where the trailing star-run + slash closer begins; {@code end} when the comment is unclosed. */
  private static int closerStart(@NotNull CharSequence buffer, int contentStart, int end) {
    if (end - contentStart < 2 || buffer.charAt(end - 1) != '/' || buffer.charAt(end - 2) != '*') {
      return end;
    }
    int closer = end - 2;
    while (closer > contentStart && buffer.charAt(closer - 1) == '*') closer--;
    return closer;
  }

  /** JavaDoc style: at least one interior line, all of them starting with a star. */
  private static boolean isStarStyle(String @NotNull [] lines) {
    if (lines.length < 3) return false;
    for (int i = 1; i < lines.length - 1; i++) {
      if (!STAR_LINE.matcher(lines[i]).matches()) return false;
    }
    return true;
  }

  /**
   * The shortest leading-whitespace run among interior lines, mirroring the
   * reference formatter: lines with no leading whitespace do not count (a
   * hard-wrapped column-0 line must not drag the prefix to nothing), and the
   * final whitespace-only line before the closer is the closer's indent, not
   * body content.
   */
  @Nullable
  private static String commonPrefix(String @NotNull [] lines) {
    String prefix = null;
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      boolean lastLine = i == lines.length - 1;
      if (lastLine && line.isBlank()) break;
      String ws = HaxeDocumentationUtil.leadingWhitespace(line);
      if (ws.isEmpty()) continue;
      if (prefix == null || prefix.length() > ws.length()) {
        prefix = ws;
      }
    }
    return prefix;
  }

  private static void segmentLine(@NotNull List<Segment> segments,
                                  @NotNull CharSequence buffer,
                                  int lineStart,
                                  @NotNull String line,
                                  boolean firstLine,
                                  boolean starStyle,
                                  @Nullable String commonPrefix) {
    if (line.isEmpty()) return;
    if (line.isBlank()) {
      addOrMergeWhitespace(segments, lineStart, lineStart + line.length());
      return;
    }

    int cursor = lineStart;
    String rest = line;
    if (!firstLine) {
      String ws = HaxeDocumentationUtil.leadingWhitespace(line);
      String managed = managedPrefix(starStyle, ws, commonPrefix);
      if (!managed.isEmpty()) {
        addOrMergeWhitespace(segments, cursor, cursor + managed.length());
        cursor += managed.length();
        rest = line.substring(managed.length());
      }
      // the classification skips the final line (mirroring the reference), so its star is not guaranteed
      if (starStyle && rest.startsWith("*")) {
        segments.add(new Segment(DOC_LEADING_ASTERISK, cursor, cursor + 1));
        cursor += 1;
        rest = rest.substring(1);
      }
    }
    segmentContent(segments, cursor, rest);
  }

  /** The line's content after any managed prefix: a leading tag becomes its own token, the rest is opaque data. */
  private static void segmentContent(@NotNull List<Segment> segments, int start, @NotNull String content) {
    if (content.isEmpty()) return;
    String afterSpaces = content.stripLeading();
    int spaces = content.length() - afterSpaces.length();
    var tagMatcher = TAG_START.matcher(afterSpaces);
    if (tagMatcher.lookingAt()) {
      if (spaces > 0) {
        segments.add(new Segment(DOC_DATA, start, start + spaces));
      }
      int tagStart = start + spaces;
      int tagEnd = tagStart + tagMatcher.end();
      segments.add(new Segment(DOC_TAG_NAME, tagStart, tagEnd));
      if (tagEnd < start + content.length()) {
        segments.add(new Segment(DOC_DATA, tagEnd, start + content.length()));
      }
      return;
    }
    segments.add(new Segment(DOC_DATA, start, start + content.length()));
  }

  private static void addOrMergeWhitespace(@NotNull List<Segment> segments, int start, int end) {
    if (!segments.isEmpty()) {
      Segment last = segments.getLast();
      if (last.type == TokenType.WHITE_SPACE && last.end == start) {
        segments.set(segments.size() - 1, new Segment(TokenType.WHITE_SPACE, last.start, end));
        return;
      }
    }
    segments.add(new Segment(TokenType.WHITE_SPACE, start, end));
  }

  /** The whitespace this lexer manages at a line start: the whole run in star style, the common prefix when the line carries it. */
  @NotNull
  private static String managedPrefix(boolean starStyle, @NotNull String leadingWhitespace, @Nullable String commonPrefix) {
    if (starStyle) return leadingWhitespace;
    if (commonPrefix == null) return "";
    return leadingWhitespace.startsWith(commonPrefix) ? commonPrefix : "";
  }
}
