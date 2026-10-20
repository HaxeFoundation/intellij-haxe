package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reformat reaches inside INACTIVE conditional branches through their lazily
 * parsed sub-trees - same rules as active code, like haxe-formatter. Token
 * soup and toggled-off branches are preserved verbatim. Byte parity with the
 * reference is pinned separately by the conditional-inactive fixture.
 */
@DisplayName("Formatting: inactive")
public class HaxeInactiveFormattingTest extends HaxeLightFixtureTestCase {
  private static final String MESSY_BRANCH_SOURCE = """
    class Main {
        static function main() {
            #if js
    trace(   "js"  ,1+2 );
            #end
            trace("live");
        }
    }
    """;
  private static final Consumer<CodeStyleSettings> ALIGN_INACTIVE =
    settings -> settings.getCustomSettings(HaxeCodeStyleSettings.class).ALIGN_INACTIVE_CONDITIONAL_BRANCHES = true;

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("dead statements format with the normal rules")
  public void testDeadStatementsFormatWithTheNormalRules() {
    assertEquals("""
      class Main {
          static function main() {
              #if js
              trace("js", 1 + 2);
              #end
              trace("live");
          }
      }
      """, reformat(MESSY_BRANCH_SOURCE));
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
  @DisplayName("if statements in dead branches get brace spacing")
  public void testIfStatementsInDeadBranchesGetBraceSpacing() {
    String source = """
      class Main {
          static function main() {
              #if js
              if(true)  {   trace("dead"); }
              #end
          }
      }
      """;

    String result = reformat(source);

    assertTrue(result.contains("if (true) {"), "dead if statements take the same brace spacing as live ones:\n" + result);
  }

  @Test
  @DisplayName("token soup branches are preserved verbatim")
  public void testTokenSoupBranchesArePreservedVerbatim() {
    String source = """
      class Main {
          static function main() {
              var x = 1 #if truthy < #else > #end 2;
          }
      }
      """;

    assertEquals(source, reformat(source), "unstructurable branches stay byte-identical");
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
  @DisplayName("nested region in a member branch follows its braces")
  public void testNestedRegionInAMemberBranchFollowsItsBraces() {
    // the function's '{' and '}' land in different fragments, so no fragment
    // parses at its true depth; the group's brace count restores it, for the
    // inner directives and the clean-parsing statements alike
    String source = """
      class Main {
          #if native
          function blend():Void {
          #if debug
          trace("debug");
          #else
          trace("release");
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
              trace("debug");
              #else
              trace("release");
              #end
          }
          #end
      }
      """, reformat(ALIGN_INACTIVE, source));
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
