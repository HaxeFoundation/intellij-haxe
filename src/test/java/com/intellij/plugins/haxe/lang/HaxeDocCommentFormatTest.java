package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Doc comment interior indentation on reformat under the plugin defaults:
 * the toggle turns the whole treatment off, and a zero-column doc still
 * follows its scope while KEEP_FIRST_COLUMN_COMMENT pins plain comments.
 * Byte parity with haxe-formatter is pinned separately by the comparison
 * suite's doc-comment-indent fixture.
 */
@DisplayName("Formatting: doc comment")
public class HaxeDocCommentFormatTest extends HaxeLightFixtureTestCase {
  private static final String COLUMN_ZERO_SOURCE = """
    class Main {
        /**
            Body line, hard-wrapped as
    wrapped at column zero

            \tdeeper markdown
        **/
        static function main() {
            trace(1);
        }
    }
    """;

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("toggle off keeps the interior untouched")
  public void testToggleOffKeepsTheInteriorUntouched() {
    Consumer<CodeStyleSettings> toggleOff =
      settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).FORMAT_DOC_COMMENTS = false;

    assertEquals(COLUMN_ZERO_SOURCE, reformat(toggleOff, COLUMN_ZERO_SOURCE));
  }

  @Test
  @DisplayName("zero column doc follows its scope while plain comments keep first column")
  public void testZeroColumnDocFollowsItsScopeWhilePlainCommentsKeepFirstColumn() {
    String source = """
      class Main {
      /**
      \tdocs for main
      **/
          static function main() {
              trace(1);
          }

      // disabled-code comment stays (keep-first-column default)
          static var x:Int = 1;
      }
      """;

    assertEquals("""
      class Main {
          /**
              docs for main
          **/
          static function main() {
              trace(1);
          }

      // disabled-code comment stays (keep-first-column default)
          static var x:Int = 1;
      }
      """, reformat(source));
  }

  @Test
  @DisplayName("javadoc stars align under the opener while the text keeps its indent")
  public void testJavadocStarsAlignUnderTheOpenerWhileTheTextKeepsItsIndent() {
    String source = """
      class Main {
      /**
      * zero-column stars
              *   deeper text after the star
      * @param x the value
      */
          static function main(x:Int) {
              trace(x);
          }
      }
      """;

    assertEquals("""
      class Main {
          /**
           * zero-column stars
           *   deeper text after the star
           * @param x the value
           */
          static function main(x:Int) {
              trace(x);
          }
      }
      """, reformat(source));
  }

  @Test
  @DisplayName("a line without a star makes the comment haxedoc style")
  public void testALineWithoutAStarMakesTheCommentHaxedocStyle() {
    String source = """
      class Main {
      /**
      \tIntro
      \t* a markdown bullet
      **/
          static function main() {
              trace(1);
          }
      }
      """;

    assertEquals("""
      class Main {
          /**
              Intro
              * a markdown bullet
          **/
          static function main() {
              trace(1);
          }
      }
      """, reformat(source));
  }
}
