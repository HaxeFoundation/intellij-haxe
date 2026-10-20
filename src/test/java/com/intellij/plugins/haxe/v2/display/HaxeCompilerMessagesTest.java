package com.intellij.plugins.haxe.v2.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The compiler quotes code in backticks; the annotation text shows quotes, the tooltip shows code. */
@DisplayName("Compiler services: compiler messages")
public class HaxeCompilerMessagesTest {

  /** (compiler message, annotation text, tooltip html - the platform escapes an apostrophe as &#39;). */
  static final List<Arguments> MESSAGES = List.of(
    arguments("Using `display` here doesn't work", "Using 'display' here doesn't work",
              "<html>Using <code>display</code> here doesn&#39;t work</html>"),
    arguments("`a` and `b` differ", "'a' and 'b' differ", "<html><code>a</code> and <code>b</code> differ</html>"),
    // the html form escapes markup inside and outside a span
    arguments("Type `Array<Int>` is not <T>", "Type 'Array<Int>' is not <T>",
              "<html>Type <code>Array&lt;Int&gt;</code> is not &lt;T&gt;</html>"),
    // an unmatched backtick is plain text
    arguments("a lone ` tick", "a lone ` tick", "<html>a lone ` tick</html>"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("MESSAGES")
  @DisplayName("code spans render as quotes in text and as code in html")
  public void codeSpansRenderAsQuotesInTextAndAsCodeInHtml(String message, String plainText, String html) {
    assertEquals(plainText, HaxeCompilerMessages.plainText(message));
    assertEquals(html, HaxeCompilerMessages.html(message));
  }
}
