package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

/** Line-oriented text helpers shared by the formatter processors. */
public final class HaxeIndentText {

  private static final String INDENT_CHARS = " \t";

  private HaxeIndentText() {
  }

  /** The indent options in force: the language's own, else the root settings' general ones. */
  @NotNull
  public static CommonCodeStyleSettings.IndentOptions indentOptions(@NotNull CommonCodeStyleSettings common) {
    CommonCodeStyleSettings.IndentOptions options = common.getIndentOptions();
    return options != null ? options : common.getRootSettings().getIndentOptions();
  }

  /** The whitespace prefix of the line containing {@code offset}. */
  public static String lineIndentAt(CharSequence text, int offset) {
    return leadingWhitespace(text, lineStartOffset(text, offset));
  }

  /** The run of spaces and tabs starting at {@code from}. */
  static String leadingWhitespace(CharSequence text, int from) {
    int end = CharArrayUtil.shiftForward(text, from, INDENT_CHARS);
    return text.subSequence(from, end).toString();
  }

  /** The offset where {@code offset}'s line begins. */
  public static int lineStartOffset(CharSequence text, int offset) {
    return CharArrayUtil.shiftBackwardUntil(text, offset - 1, "\n") + 1;
  }

  /** The column the whitespace reaches, tabs advancing to the next tab stop. */
  public static int indentWidth(String whitespace, int tabSize) {
    int columns = 0;
    for (int i = 0; i < whitespace.length(); i++) {
      columns = whitespace.charAt(i) == '\t' ? (columns / tabSize + 1) * tabSize : columns + 1;
    }
    return columns;
  }
}
