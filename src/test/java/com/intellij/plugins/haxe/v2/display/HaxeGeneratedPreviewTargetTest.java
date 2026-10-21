package com.intellij.plugins.haxe.v2.display;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.HaxeTargetElementEvaluator;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.pom.Navigatable;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Navigation: generated preview target")
public class HaxeGeneratedPreviewTargetTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    // the fixture is built from text; no test-data directory
    return "";
  }

  /**
   * The implementation popup wraps targets in smart pointers and navigates the
   * dereferenced element through EditSourceUtil, which builds a file/offset
   * descriptor for plain PSI elements without calling their navigate(). The
   * preview target must come back as ITS OWN navigatable from that path, or
   * choosing its popup entry opens the previewed declaration's file instead
   * of the preview.
   */
  @Test
  @DisplayName("edit source descriptor is the target itself")
  public void testEditSourceDescriptorIsTheTargetItself() {
    PsiFile file = myFixture.configureByText("Sample.hx", "extern class Sample { function run():Void; }");
    HaxeClass externClass = PsiTreeUtil.findChildOfType(file, HaxeClass.class);
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    var context = new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");

    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.ofType("Sample", "run");
    PsiElement element = HaxeGeneratedPreviewTarget.createElement(externClass, context, spot);
    Navigatable descriptor = EditSourceUtil.getDescriptor(element);

    HaxeGeneratedPreviewTarget target = assertInstanceOf(HaxeGeneratedPreviewTarget.class, descriptor);
    assertSame(((PomTargetPsiElement)element).getTarget(), target);
    assertTrue(target.canNavigate(), "the popup only navigates targets that canNavigate");
  }

  /**
   * Go-to-declaration navigates to a target's navigation element itself,
   * without calling navigate(); the evaluator must hand the wrapper back as
   * the target, or the caret lands on the previewed declaration instead of
   * the preview opening.
   */
  @Test
  @DisplayName("go to declaration keeps the wrapper as its target")
  public void testGoToDeclarationKeepsTheWrapperAsItsTarget() {
    PsiFile file = myFixture.configureByText("Sample.hx", "class Sample { static var field = 1; }");
    HaxeClass sampleClass = PsiTreeUtil.findChildOfType(file, HaxeClass.class);
    var overrides = HaxeDisplayConfiguration.OverrideArguments.EMPTY;
    var context = new HaxeCompilerDisplayService.DisplayContext(List.of(), null, null, overrides, "container");
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.ofType("Sample", "field");
    PsiElement element = HaxeGeneratedPreviewTarget.createElement(sampleClass, context, spot);

    PsiElement target = new HaxeTargetElementEvaluator().getGotoDeclarationTarget(element, element.getNavigationElement());

    assertSame(element, target);
    assertSame(sampleClass, element.getNavigationElement(), "popups still present the previewed declaration");
  }
}
