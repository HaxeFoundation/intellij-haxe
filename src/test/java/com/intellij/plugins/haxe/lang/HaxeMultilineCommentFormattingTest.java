package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The interior lines of a plain multi-line comment live INSIDE its token,
 * out of block formatting's reach; REINDENT_MULTILINE_COMMENTS (default on)
 * reindents them hxformat-style, off restores the IntelliJ convention of
 * leaving interiors alone. Exact parity is pinned by the multiline-comments
 * comparison fixture.
 */
@DisplayName("Formatting: multiline comment")
public class HaxeMultilineCommentFormattingTest extends HaxeLightFixtureTestCase {
  // the comment sits OFF column 0 - a first-column comment is pinned by
  // KEEP_FIRST_COLUMN_COMMENT under default settings and would not move
  private static final String MISALIGNED_COMMENT_SOURCE = """
    class Main {
        static function main() {
      /* step one
         step two */
            trace("live");
        }
    }
    """;

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("comment interiors reindent by default")
  public void testCommentInteriorsReindentByDefault() {
    String result = reformat(MISALIGNED_COMMENT_SOURCE);

    // the comment moved to scope indent (8) and its interior sits one level
    // deeper with the common margin stripped
    assertTrue(result.contains("        /* step one"), "the opener takes the scope indent:\n" + result);
    assertTrue(result.contains("            step two */"), "interior lines reindent one level deeper:\n" + result);
  }

  @Test
  @DisplayName("toggle off keeps comment interiors untouched")
  public void testToggleOffKeepsCommentInteriorsUntouched() {
    Consumer<CodeStyleSettings> toggleOff =
      settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).REINDENT_MULTILINE_COMMENTS = false;

    String result = reformat(toggleOff, MISALIGNED_COMMENT_SOURCE);

    assertTrue(result.contains("   step two */"), "interior lines keep their columns:\n" + result);
  }

  @Test
  @DisplayName("first column comment stays fully untouched when pinned")
  public void testFirstColumnCommentStaysFullyUntouchedWhenPinned() {
    // KEEP_FIRST_COLUMN_COMMENT (on by default) pins the opener at the
    // margin - shifting only the interior would leave the comment half done
    String source = """
      class Main {
          static function main() {
      /* step one
         step two */
              trace("live");
          }
      }
      """;

    String result = reformat(source);

    assertTrue(result.contains("\n/* step one\n   step two */\n"),
               "a pinned margin comment keeps opener AND interior:\n" + result);
  }
}
