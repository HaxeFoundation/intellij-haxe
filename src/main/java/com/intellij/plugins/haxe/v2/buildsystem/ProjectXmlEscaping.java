package com.intellij.plugins.haxe.v2.buildsystem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Makes a Lime, OpenFL or NME project file readable for a conforming XML
 * parser. The tools read their files with Haxe's lenient Xml parser, which
 * takes an attribute value up to its closing quote, so a condition such as
 * {@code if="${haxe_ver < 4.3}"} carries a raw {@code <} - and a value may
 * carry a bare {@code &} - although XML forbids both inside an attribute value.
 */
final class ProjectXmlEscaping {

  /** A stretch the escaping copies as it is, from its opening to its closing marker. */
  private record Verbatim(@NotNull String open, @NotNull String close) {
  }

  // the comment and CDATA openers come before the declaration's: all three start with "<!"
  private static final List<Verbatim> VERBATIM_SECTIONS = List.of(
    new Verbatim("<!--", "-->"),
    new Verbatim("<![CDATA[", "]]>"),
    new Verbatim("<?", "?>"),
    new Verbatim("<!", ">"));

  // an entity or character reference at the match start: &name; &#60; &#x3C;
  private static final Pattern REFERENCE = Pattern.compile("&(?:[A-Za-z_][\\w.-]*|#\\d+|#x\\p{XDigit}+);");

  private ProjectXmlEscaping() {
  }

  /** The text with every {@code <} and bare {@code &} inside an attribute value escaped; comments, CDATA and declarations stay as they are. */
  @NotNull
  static String escapeAttributeValues(@NotNull String xml) {
    StringBuilder escaped = new StringBuilder(xml.length() + 16);
    boolean inTag = false;
    char quote = 0;
    int index = 0;
    while (index < xml.length()) {
      char c = xml.charAt(index);
      if (quote != 0) {
        if (c == quote) quote = 0;
        appendValueCharacter(xml, index, escaped);
        index++;
        continue;
      }
      if (inTag) {
        if (c == '"' || c == '\'') quote = c;
        inTag = c != '>';
        escaped.append(c);
        index++;
        continue;
      }
      int verbatimEnd = verbatimEnd(xml, index);
      if (verbatimEnd > index) {
        escaped.append(xml, index, verbatimEnd);
        index = verbatimEnd;
        continue;
      }
      inTag = c == '<';
      escaped.append(c);
      index++;
    }
    return escaped.toString();
  }

  private static void appendValueCharacter(@NotNull String xml, int index, @NotNull StringBuilder escaped) {
    char c = xml.charAt(index);
    if (c == '<') {
      escaped.append("&lt;");
    }
    else if (c == '&' && !REFERENCE.matcher(xml).region(index, xml.length()).lookingAt()) {
      escaped.append("&amp;");
    }
    else {
      escaped.append(c);
    }
  }

  /** Where a comment, CDATA section, processing instruction or declaration opening at {@code index} ends (an unterminated one runs to the end); {@code index} when none opens there. */
  private static int verbatimEnd(@NotNull String xml, int index) {
    for (Verbatim verbatim : VERBATIM_SECTIONS) {
      if (!xml.startsWith(verbatim.open(), index)) continue;
      int close = xml.indexOf(verbatim.close(), index + verbatim.open().length());
      return close < 0 ? xml.length() : close + verbatim.close().length();
    }
    return index;
  }
}
