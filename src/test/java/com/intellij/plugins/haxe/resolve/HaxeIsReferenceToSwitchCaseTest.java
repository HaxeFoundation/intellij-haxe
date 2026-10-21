package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeIsReferenceToUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The isReferenceTo fast path (HaxeIsReferenceToUtil) against the full
 * resolver for a bare lowercase identifier in a switch case that collides with
 * an enum value of the switched type - the spot where the fast path's check
 * order differs most from HaxeResolver's pipeline. The contract under test:
 * whenever the fast path answers (non-null), its answer equals what full
 * resolve implies.
 */
@DisplayName("Resolve: isReferenceTo switch case fast path")
public class HaxeIsReferenceToSwitchCaseTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private HaxeReference caseReference() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeReference reference = PsiTreeUtil.getParentOfType(leaf, HaxeReference.class);
    assertNotNull(reference, "reference at caret");
    return reference;
  }

  private HaxeComponentName localVarNamed(String name) {
    Collection<HaxeComponentName> names = PsiTreeUtil.findChildrenOfType(myFixture.getFile(), HaxeComponentName.class);
    for (HaxeComponentName candidate : names) {
      if (name.equals(candidate.getText()) && candidate.getParent() instanceof HaxeLocalVarDeclaration) {
        return candidate;
      }
    }
    fail("no local var named " + name);
    return null;
  }

  @Test
  @DisplayName("bare case identifier matching enum value is not claimed for an unrelated local")
  public void testBareCaseIdentifierMatchingEnumValueIsNotClaimedForUnrelatedLocal() {
    myFixture.configureByText("Main.hx", """
      enum Foo { lower; Other; }
      class Main {
        static function main() {
          var probe = 1;
          var f:Foo = Other;
          switch (f) {
            case low<caret>er: trace(probe);
            case _: trace(probe);
          }
        }
      }""");
    HaxeReference reference = caseReference();
    HaxeComponentName probe = localVarNamed("probe");

    Boolean fastAnswer = HaxeIsReferenceToUtil.tryIsReferenceTo(reference, probe);
    assertNotNull(fastAnswer, "fast path must answer for a bare case identifier");
    assertFalse(fastAnswer, "bare case identifier is not a reference to an unrelated local");

    PsiElement fullResolve = reference.resolveToComponentName();
    assertNotNull(fullResolve, "full resolve must find the enum value");
    HaxeEnumDeclaration enclosingEnum = PsiTreeUtil.getParentOfType(fullResolve, HaxeEnumDeclaration.class);
    assertNotNull(enclosingEnum, "full resolve must land on the enum value, not a local");
    assertEquals(fullResolve == probe, fastAnswer.booleanValue(), "fast path must agree with full resolve");
  }

  @Test
  @DisplayName("bare case identifier shadowed by local agrees with full resolve")
  public void testBareCaseIdentifierShadowedByLocalAgreesWithFullResolve() {
    // both pipelines answer this via the tree walk (which runs before any enum
    // check in each); the assertion pins agreement, not a preferred candidate
    myFixture.configureByText("Main.hx", """
      enum Foo { lower; Other; }
      class Main {
        static function main() {
          var lower = 1;
          var f:Foo = Other;
          switch (f) {
            case low<caret>er: trace(0);
            case _: trace(0);
          }
        }
      }""");
    HaxeReference reference = caseReference();
    HaxeComponentName local = localVarNamed("lower");

    Boolean fastAnswer = HaxeIsReferenceToUtil.tryIsReferenceTo(reference, local);
    assertNotNull(fastAnswer, "fast path must answer for a bare case identifier");

    PsiElement fullResolve = reference.resolveToComponentName();
    assertEquals(fullResolve == local, fastAnswer.booleanValue(), "fast path must agree with full resolve");
  }
}
