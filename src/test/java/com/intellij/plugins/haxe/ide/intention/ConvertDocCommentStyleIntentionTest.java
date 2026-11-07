package com.intellij.plugins.haxe.ide.intention;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Intention: convert doc comment style")
public class ConvertDocCommentStyleIntentionTest extends HaxeLightFixtureTestCase {
  private static final String TO_HAXE_STYLE = HaxeBundle.message("haxe.intention.convert.doc.comment.to.haxe.style");
  private static final String TO_JAVADOC_STYLE = HaxeBundle.message("haxe.intention.convert.doc.comment.to.javadoc.style");

  static final List<Arguments> CONVERSIONS = List.of(
    arguments("javadoc style to haxe style", TO_HAXE_STYLE, """
      class Test {
          /**
           * Sums <caret>two values.
           *
           * @param a the first
           *          value
           */
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """, """
      class Test {
          /**
              Sums two values.

              @param a the first
                       value
          **/
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """),
    arguments("haxe style to javadoc style", TO_JAVADOC_STYLE, """
      class Test {
          /**
              Sums <caret>two values.

              @param a the first
                       value
          **/
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """, """
      class Test {
          /**
           * Sums two values.
           *
           * @param a the first
           *          value
           */
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """),
    arguments("text on the opener's line stays there", TO_HAXE_STYLE, """
      class Test {
          /** Sums <caret>two values.
           * Both are integers.
           */
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """, """
      class Test {
          /** Sums two values.
              Both are integers.
          **/
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """),
    arguments("text in front of the closer becomes a body line", TO_JAVADOC_STYLE, """
      class Test {
          /**
              Sums <caret>two values. **/
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """, """
      class Test {
          /**
           * Sums two values.
           */
          function sum(a:Int, b:Int):Int { return a + b; }
      }
      """),
    arguments("a one-line javadoc comment changes its closer", TO_HAXE_STYLE,
              "class Test {\n    /** The <caret>count. */\n    var count:Int;\n}",
              "class Test {\n    /** The count. **/\n    var count:Int;\n}"),
    arguments("a one-line haxe comment changes its closer", TO_JAVADOC_STYLE,
              "class Test {\n    /** The <caret>count. **/\n    var count:Int;\n}",
              "class Test {\n    /** The count. */\n    var count:Int;\n}"));

  @Override
  protected String getBasePath() {
    return "";
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("CONVERSIONS")
  @DisplayName("converts a doc comment to the other style")
  public void testConvertsADocCommentToTheOtherStyle(String name, String action, String before, String after) {
    myFixture.configureByText("Test.hx", before);

    myFixture.launchAction(myFixture.findSingleIntention(action));

    myFixture.checkResult(after);
  }

  @Test
  @DisplayName("the preview shows the whole converted comment")
  public void testThePreviewShowsTheWholeConvertedComment() {
    myFixture.configureByText("Test.hx", """
      class Test {
          /**
           *
           * @re<caret>turn
           */
          function count():Int { return 0; }
      }
      """);

    String preview = myFixture.getIntentionPreviewText(myFixture.findSingleIntention(TO_HAXE_STYLE));

    assertEquals("""
      /**

          @return
      **/""", preview.stripIndent());
  }

  @Test
  @DisplayName("a block comment offers no conversion")
  public void testABlockCommentOffersNoConversion() {
    myFixture.configureByText("Test.hx", """
      class Test {
          /*
           * Not <caret>documentation.
           */
          var count:Int;
      }
      """);

    assertTrue(myFixture.filterAvailableIntentions(TO_HAXE_STYLE).isEmpty());
    assertTrue(myFixture.filterAvailableIntentions(TO_JAVADOC_STYLE).isEmpty());
  }
}
