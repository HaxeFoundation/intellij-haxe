package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclarationConstructor;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * An enum constructor in a switch pattern resolves at the component-name
 * level, like every other resolver check: the resolve cache stores that
 * level, and a check answering with the declaration instead makes the same
 * reference resolve differently depending on which check answered, which the
 * platform reports as a non-idempotent resolve.
 */
@DisplayName("Resolve: enum pattern resolve level")
public class HaxeEnumPatternResolveLevelTest extends HaxeLightFixtureTestCase {

  private static final String PATTERN_SOURCE = """
    enum Verdict {
      Pass(value:Int);
      Fail(message:String);
    }
    typedef Report = Verdict;
    class Main {
      static function check():Report {
        return Pass(0);
      }
      static function main() {
        switch check() {
          case Fa<caret>il(message):
            trace(message);
          case Pass(_):
        }
      }
    }
    """;

  private static final String NESTED_PATTERN_SOURCE = """
    enum Verdict {
      Pass(value:Int);
      Fail(message:String);
    }
    enum Envelope {
      Wrap(verdict:Verdict);
    }
    class Main {
      static function open():Envelope {
        return Wrap(Pass(0));
      }
      static function main() {
        switch open() {
          case Wrap(Fa<caret>il(message)):
            trace(message);
          case Wrap(_):
        }
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("an enum pattern resolves to the constructor name")
  public void testAnEnumPatternResolvesToTheConstructorName() {
    myFixture.configureByText("Main.hx", PATTERN_SOURCE);

    assertResolvesToFailName(caseReference());
  }

  @Test
  @DisplayName("a nested enum pattern resolves to the constructor name")
  public void testANestedEnumPatternResolvesToTheConstructorName() {
    myFixture.configureByText("Main.hx", NESTED_PATTERN_SOURCE);

    assertResolvesToFailName(caseReference());
  }

  private HaxeReference caseReference() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeReference reference = PsiTreeUtil.getParentOfType(leaf, HaxeReference.class);
    assertNotNull(reference, "reference at caret");
    return reference;
  }

  /** The resolver's own answer is the constructor's name; resolve() still hands callers the declaration. */
  private void assertResolvesToFailName(HaxeReference reference) {
    List<? extends PsiElement> cached = HaxeResolver.getInstance(getProject()).resolve(reference, true);

    assertEquals(1, cached.size(), "one target, got: " + cached);
    HaxeComponentName name = assertInstanceOf(HaxeComponentName.class, cached.getFirst());
    assertEquals("Fail", name.getText());
    assertInstanceOf(HaxeEnumValueDeclarationConstructor.class, reference.resolve());
  }
}
