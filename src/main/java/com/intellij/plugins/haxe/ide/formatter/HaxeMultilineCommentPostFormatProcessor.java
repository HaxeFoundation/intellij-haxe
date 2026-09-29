package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.MML_COMMENT;

/**
 * Reindents the interior lines of plain multi-line comments, as
 * haxe-formatter's MarkTokenText.printComment does:
 * <ul>
 * <li>leading whitespace is converted to the indent character;</li>
 * <li>the shortest margin common to the lines is removed;</li>
 * <li>middle lines sit one level deeper than the comment;</li>
 * <li>the closing line returns to the comment's level.</li>
 * </ul>
 * A comment has a "star rail" when every middle line starts with a '*'.
 * Those stars then align under the first star of the opening "/*".
 * <p>
 * Doc comments are formatted by their own line blocks, and single-line
 * comments have no interior. Turning the setting off restores the IntelliJ
 * convention of leaving comment interiors alone.
 */
public class HaxeMultilineCommentPostFormatProcessor extends HaxeTextPostFormatProcessor {

  // a middle line on a star rail: whitespace, a star, then whitespace or the line end
  private static final Pattern STAR_RAIL_LINE = Pattern.compile("^\\s*\\*(\\s|$)");
  // a closing line holding nothing but stars, or starting with a '}';
  // such a line does not count toward the common margin
  private static final Pattern CLOSING_ONLY_LINE = Pattern.compile("^\\s*(\\**$|\\})");
  // a closing line that is only stars (the classic "**/" ending)
  private static final Pattern STARS_ONLY_LINE = Pattern.compile("^\\s*\\*\\**$");

  @Override
  protected boolean enabled(@NotNull HaxeCodeStyleSettings settings) {
    return settings.REINDENT_MULTILINE_COMMENTS;
  }

  @Override
  protected boolean handles(@NotNull ASTNode node) {
    return node.getElementType() == MML_COMMENT && node.textContains('\n');
  }

  @Override
  protected @NotNull List<Replacement> replacements(@NotNull List<ASTNode> comments, @NotNull Pass pass) {
    CommonCodeStyleSettings.IndentOptions options = pass.indentOptions();
    boolean keepFirstColumn = pass.settings().getCommonSettings(HaxeLanguage.INSTANCE).KEEP_FIRST_COLUMN_COMMENT;
    String documentText = pass.text();

    List<Replacement> replacements = new ArrayList<>();
    for (ASTNode comment : comments) {
      if (!pass.editable(comment)) continue;
      int start = comment.getStartOffset();
      // Block formatting kept this comment at the first column on purpose,
      // so its interior stays as written too.
      boolean pinnedAtFirstColumn = keepFirstColumn && HaxeIndentText.lineStartOffset(documentText, start) == start;
      if (pinnedAtFirstColumn) continue;
      String text = documentText.substring(start, start + comment.getTextLength());
      String baseIndent = HaxeIndentText.lineIndentAt(documentText, start);
      String reindented = reindent(text, baseIndent, options);
      if (!reindented.equals(text)) {
        replacements.add(new Replacement(comment, reindented));
      }
    }
    return replacements;
  }

  private static String reindent(String text, String baseIndent, CommonCodeStyleSettings.IndentOptions options) {
    if (!text.startsWith("/*") || !text.endsWith("*/") || text.length() < 4) return text;
    String content = text.substring(2, text.length() - 2);
    // every line, trailing empty ones kept
    String[] lines = content.split("\n", -1);
    if (lines.length < 2) return text;
    boolean starRailed = hasStarRail(lines);

    for (int i = 0; i < lines.length; i++) {
      lines[i] = convertLeadingIndent(lines[i], options);
    }
    removeCommonMargin(lines);

    String unit = indentUnit(options);
    int last = lines.length - 1;
    StringBuilder out = new StringBuilder("/*").append(lines[0]);
    for (int i = 1; i <= last; i++) {
      String formatted = i == last
                         ? formatClosingLine(lines[i], baseIndent, unit)
                         : formatMiddleLine(lines[i], baseIndent, unit, starRailed);
      out.append('\n').append(formatted);
    }
    return out.append("*/").toString();
  }

  /** Whether the comment has a star rail: at least one middle line, and every middle line starts with a '*'. */
  private static boolean hasStarRail(String[] lines) {
    if (lines.length < 3) return false;
    for (int i = 1; i < lines.length - 1; i++) {
      if (!STAR_RAIL_LINE.matcher(lines[i]).find()) return false;
    }
    return true;
  }

  /**
   * A middle line with its indent. The line sits one level in from the
   * comment; on a star rail it gets one space instead, which puts its star
   * under the opener's. An empty line gets no indent.
   */
  private static String formatMiddleLine(String line, String baseIndent, String unit, boolean starRailed) {
    String text = starRailed ? " " + line : line;
    if (line.isEmpty()) return text;
    String lineIndent = starRailed ? baseIndent : baseIndent + unit;
    return lineIndent + text;
  }

  /**
   * The closing line with its indent. A line starting with '}' only moves to
   * the comment's indent. Otherwise plain text sits one level in, and a line
   * that is empty or starts with a '*' stays at the comment's indent. A '*'
   * followed by text gets one extra space, which puts it under the opener's
   * star. Unless the line then ends with a '*', a space separates it from
   * the closing "*&#47;".
   */
  private static String formatClosingLine(String line, String baseIndent, String unit) {
    String body = line.stripLeading();
    if (body.startsWith("}")) return baseIndent + body.stripTrailing();
    boolean plainText = !body.isEmpty() && body.charAt(0) != '*';
    String lineIndent = plainText ? baseIndent + unit : baseIndent;
    String text = isStarThenText(body) ? " " + line : line;
    text = text.stripTrailing();
    if (!text.endsWith("*")) {
      text = text + " ";
    }
    return lineIndent + text;
  }

  /** Whether the line is a star followed by text, like "* done"; not "**" or a lone star. */
  private static boolean isStarThenText(String body) {
    if (!body.startsWith("*")) return false;
    String afterStar = body.substring(1).stripLeading();
    return !afterStar.isEmpty() && afterStar.charAt(0) != '*';
  }

  /**
   * Finds the shortest non-empty leading whitespace among the interior lines
   * and strips that margin from every line. A line starting with the margin,
   * a space and a star loses the margin and the space but keeps its star.
   */
  private static void removeCommonMargin(String[] lines) {
    int endIndex = lines.length - 1;
    if (!CLOSING_ONLY_LINE.matcher(lines[lines.length - 1]).find()) {
      endIndex = lines.length;
    }
    String margin = null;
    for (int i = 1; i < endIndex; i++) {
      String lead = HaxeIndentText.leadingWhitespace(lines[i], 0);
      if (lead.isEmpty()) continue;
      if (margin == null || margin.length() > lead.length()) {
        margin = lead;
      }
    }
    if (margin != null) {
      String starMargin = margin + " *";
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (line.startsWith(starMargin)) {
          line = line.substring(starMargin.length() - 1);
        }
        if (line.startsWith(margin)) {
          line = line.substring(margin.length());
        }
        lines[i] = line;
      }
    }
    String last = lines[lines.length - 1];
    if (STARS_ONLY_LINE.matcher(last).find()) {
      lines[lines.length - 1] = last.stripLeading();
    }
  }

  /**
   * One indent level inside a comment. There one tab counts as one level,
   * never as TAB_SIZE columns, so a tab-indented level is a single tab.
   * {@link #convertLeadingIndent} converts by the same rule.
   */
  private static String indentUnit(CommonCodeStyleSettings.IndentOptions options) {
    return options.USE_TAB_CHARACTER ? "\t" : " ".repeat(options.INDENT_SIZE);
  }

  /**
   * Converts a line's leading whitespace to the indent character, by the
   * one-tab-per-level rule of {@link #indentUnit}. With tabs, each run of
   * TAB_SIZE spaces becomes one tab. With spaces, each tab becomes one
   * indent level of INDENT_SIZE spaces.
   */
  private static String convertLeadingIndent(String line, CommonCodeStyleSettings.IndentOptions options) {
    String lead = HaxeIndentText.leadingWhitespace(line, 0);
    if (lead.isEmpty()) return line;
    String spaceRun = " ".repeat(options.TAB_SIZE);
    String converted = options.USE_TAB_CHARACTER
                       ? lead.replace(spaceRun, "\t")
                       : lead.replace("\t", " ".repeat(options.INDENT_SIZE));
    return converted + line.substring(lead.length());
  }
}
