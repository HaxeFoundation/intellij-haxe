package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The verbatim side of INACTIVE conditional branches on reformat: token soup
 * and toggled-off branches keep their text, and the alignment pass moves
 * such a branch as one group to its directive. Formatting inside a parsed
 * branch (same rules as active code, like haxe-formatter) is pinned by the
 * conditional-inactive and conditional-nested-inactive fixtures.
 */
@DisplayName("Formatting: inactive")
public class HaxeInactiveFormattingTest extends HaxeLightFixtureTestCase {
  private static final Consumer<CodeStyleSettings> ALIGN_INACTIVE =
    settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).ALIGN_INACTIVE_CONDITIONAL_BRANCHES = true;

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("toggle off preserves the branch verbatim")
  public void testToggleOffPreservesTheBranchVerbatim() {
    Consumer<CodeStyleSettings> toggleOff = settings -> {
      HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
      haxe.FORMAT_INACTIVE_BRANCHES = false;
      // the comment passes rewrite comment text; a preserved branch is off limits to them too
      haxe.ADD_LINE_COMMENT_SPACE = true;
      haxe.REINDENT_MULTILINE_COMMENTS = true;
    };
    String source = """
      class Main {
          static function main() {
              #if js
      trace(   "js"  ,1+2 );
      //js only
          /* first
           second */
              #end
              //live
          }
      }
      """;

    String result = reformat(toggleOff, source);

    assertTrue(result.contains("trace(   \"js\"  ,1+2 );"), "the branch text must stay untouched:\n" + result);
    assertTrue(result.contains("\n//js only\n"), "a line comment in the branch keeps its shape:\n" + result);
    assertTrue(result.contains("\n    /* first\n     second */\n"), "a block comment in the branch keeps its shape:\n" + result);
    assertTrue(result.contains("// live"), "comments outside the branch still normalize:\n" + result);
  }

  @Test
  @DisplayName("token soup lines align as a group to the directive")
  public void testTokenSoupLinesAlignAsAGroupToTheDirective() {
    // "1 +" followed by ";" parses nowhere, so the branch is preserved
    // verbatim and only the alignment pass may touch its lines
    String source = """
      class Main {
          static function main() {
              #if js
        var x = 1 +
            ;
              #end
              trace("live");
          }
      }
      """;

    String result = reformat(ALIGN_INACTIVE, source);

    assertTrue(result.contains("\n        #if js\n        var x = 1 +\n            ;\n        #end\n"),
               "the blob shifts as a whole to the directive's indent, its own nesting kept:\n" + result);
  }

  @Test
  @DisplayName("nested region moves with its verbatim branch")
  public void testNestedRegionMovesWithItsVerbatimBranch() {
    Consumer<CodeStyleSettings> alignVerbatim = ALIGN_INACTIVE.andThen(
      settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).FORMAT_INACTIVE_BRANCHES = false);
    // toggled off, the branch is verbatim: the nested region lies inside its
    // blob, so its lines move with the branch as one group to the directive's
    // indent, their text and relative nesting untouched
    String source = """
      class Main {
          #if native
      function blend():Void {
      #if debug
      var x = 1 +
          ;
      #else
      trace(   "release"  );
      #end
      }
          #end
      }
      """;

    assertEquals("""
      class Main {
          #if native
          function blend():Void {
          #if debug
          var x = 1 +
              ;
          #else
          trace(   "release"  );
          #end
          }
          #end
      }
      """, reformat(alignVerbatim, source));
  }
}
