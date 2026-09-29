package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.util.HaxeTestDefines;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The compiler's target-specific files in resolution: {@code Net.js.hx}
 * declares module {@code Net} and shadows a plain {@code Net.hx} exactly when
 * its variant is the active platform. Activation is define-driven (each stock
 * target defines its own name; a custom target sets {@code target.name}), so
 * the tests drive it through the project's user defines.
 */
@DisplayName("Resolve: module variants")
public class HaxeModuleVariantResolveTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project keeps project-level state between tests
      HaxeTestDefines.set(myFixture.getProject(), null);
    }
    finally {
      super.tearDown();
    }
  }

  private void setDefines(String defines) {
    HaxeTestDefines.set(myFixture.getProject(), defines);
  }

  /** Resolves the type reference at the caret and returns the class it lands on. */
  private HaxeClass resolveCaretToClass() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeReference reference = PsiTreeUtil.getParentOfType(leaf, HaxeReference.class);
    assertNotNull(reference, "reference at caret");
    PsiElement componentName = reference.resolveToComponentName();
    assertNotNull(componentName, "the reference must resolve");
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(componentName, HaxeClass.class);
    assertNotNull(haxeClass, "the reference must resolve to a class");
    return haxeClass;
  }

  @Test
  @DisplayName("active variant shadows the plain module")
  public void testActiveVariantShadowsThePlainModule() {
    setDefines("js");
    myFixture.addFileToProject("Net.hx", "class Net { }");
    myFixture.addFileToProject("Net.js.hx", "class Net { }");
    myFixture.configureByText("UseNet.hx", "class UseNet { var connection:N<caret>et; }");

    HaxeClass resolved = resolveCaretToClass();
    assertEquals("Net.js.hx", resolved.getContainingFile().getName());
  }

  @Test
  @DisplayName("inactive variant loses to the plain module")
  public void testInactiveVariantLosesToThePlainModule() {
    myFixture.addFileToProject("Net.hx", "class Net { }");
    myFixture.addFileToProject("Net.js.hx", "class Net { }");
    myFixture.configureByText("UseNet.hx", "class UseNet { var connection:N<caret>et; }");

    HaxeClass resolved = resolveCaretToClass();
    assertEquals("Net.hx", resolved.getContainingFile().getName());
  }

  @Test
  @DisplayName("custom target activates its variant through target name")
  public void testCustomTargetActivatesItsVariantThroughTargetName() {
    // the container's Custom target setting and hxml's --custom-target both
    // surface as this define; a bare "-D go" must NOT activate the variant
    setDefines("target.name=go");
    myFixture.addFileToProject("Util.hx", "class Util { }");
    myFixture.addFileToProject("Util.go.hx", "class Util { }");
    myFixture.configureByText("UseUtil.hx", "class UseUtil { var helper:U<caret>til; }");

    HaxeClass resolved = resolveCaretToClass();
    assertEquals("Util.go.hx", resolved.getContainingFile().getName());
  }

  @Test
  @DisplayName("variant class carries the base module qualified name")
  public void testVariantClassCarriesTheBaseModuleQualifiedName() {
    // naming is unconditional (no active variant here): stubs and indexes are
    // application-wide, so activity may never influence an indexed name
    myFixture.addFileToProject("Rec.go.hx", "class Rec { }");
    myFixture.configureByText("UseRec.hx", "class UseRec { var record:R<caret>ec; }");

    HaxeClass resolved = resolveCaretToClass();
    assertEquals("Rec.go.hx", resolved.getContainingFile().getName());
    assertEquals("Rec.Rec", resolved.getFullyQualifiedName());
  }

  @Test
  @DisplayName("variant without a plain module resolves even when inactive")
  public void testVariantWithoutAPlainModuleResolvesEvenWhenInactive() {
    // deliberate leniency: with no competing candidate the variant file is the
    // module, so an unconfigured project still resolves instead of erroring
    myFixture.addFileToProject("Only.go.hx", "class Only { }");
    myFixture.configureByText("UseOnly.hx", "class UseOnly { var single:O<caret>nly; }");

    HaxeClass resolved = resolveCaretToClass();
    assertEquals("Only.go.hx", resolved.getContainingFile().getName());
  }
}
