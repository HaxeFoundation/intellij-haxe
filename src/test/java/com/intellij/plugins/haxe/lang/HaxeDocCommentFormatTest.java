package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Doc comment interior indentation on reformat: under-indented lines rise to
 * the body indent, markdown depth beyond it stays, and the toggle turns the
 * whole treatment off. Byte parity with haxe-formatter is pinned separately
 * by the comparison suite's doc-comment-indent fixture.
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
  @DisplayName("interior lines rise to the body indent")
  public void testInteriorLinesRiseToTheBodyIndent() {
    assertEquals("""
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
      """, reformat(COLUMN_ZERO_SOURCE));
  }

  @Test
  @DisplayName("toggle off keeps the interior untouched")
  public void testToggleOffKeepsTheInteriorUntouched() {
    Consumer<CodeStyleSettings> toggleOff =
      settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).FORMAT_DOC_COMMENTS = false;

    assertEquals(COLUMN_ZERO_SOURCE, reformat(toggleOff, COLUMN_ZERO_SOURCE));
  }

  @Test
  @DisplayName("starred style aligns stars and closer one space in")
  public void testStarredStyleAlignsStarsAndCloserOneSpaceIn() {
    String source = """
      class Main {
          /**
      * Starred body.
              * @param x value
          */
          static function main(x:Int) {
              trace(x);
          }
      }
      """;

    assertEquals("""
      class Main {
          /**
           * Starred body.
           * @param x value
           */
          static function main(x:Int) {
              trace(x);
          }
      }
      """, reformat(source));
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
}
