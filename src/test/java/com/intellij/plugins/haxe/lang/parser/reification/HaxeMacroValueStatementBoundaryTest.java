package com.intellij.plugins.haxe.lang.parser.reification;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A statement whose value is a {@code macro} reification of a statement
 * ({@code var e1 = macro var $s = $e2;}) ends at its semicolon; the next
 * statement may itself start with {@code macro}. The local-var recovery set
 * stops at every statement starter, and {@code macro} is one.
 */
@DisplayName("Parsing: macro value statement boundary")
public class HaxeMacroValueStatementBoundaryTest extends HaxeLightFixtureTestCase {

  /** The shape of hxparse's ParserBuilderImpl.makePattern. */
  private static final String TERNARY_THEN_BLOCK_SOURCE = """
    class Main {
      static function makePattern(s:String, e:Expr, e2:Expr, def:Expr) {
        return if (def == null) {
          var e1 = s == "_" ? e2 : macro var $s = $e2;
          macro {
            $e1;
            $e;
          }
        } else {
          e;
        }
      }
    }
    """;

  private static final String TWO_STATEMENTS_SOURCE = """
    class Main {
      static function build(s:String, e:Expr, e2:Expr) {
        var e1 = macro var $s = $e2;
        macro {
          $e1;
          $e;
        }
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a macro var value in a ternary followed by a macro block parses")
  public void testAMacroVarValueInATernaryFollowedByAMacroBlockParses() {
    PsiFile file = myFixture.configureByText("Main.hx", TERNARY_THEN_BLOCK_SOURCE);

    assertNull(PsiTreeUtil.findChildOfType(file, PsiErrorElement.class), "parse error");
  }

  @Test
  @DisplayName("a macro var value followed by a macro block parses")
  public void testAMacroVarValueFollowedByAMacroBlockParses() {
    PsiFile file = myFixture.configureByText("Main.hx", TWO_STATEMENTS_SOURCE);

    assertNull(PsiTreeUtil.findChildOfType(file, PsiErrorElement.class), "parse error");
  }
}
