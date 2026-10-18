package com.intellij.plugins.haxe.runner.debugger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionCodeFragment;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * Name resolution inside the debugger's expression fragments (Evaluate,
 * Watches, Set Value): a bare class name resolves through the breakpoint
 * file's imports, and an auto-import is held on the fragment, never written
 * into the evaluated text.
 */
@DisplayName("Debugger: expression code fragment")
public class HaxeExpressionCodeFragmentTest extends HaxeLightFixtureTestCase {
  private static final String IMPORTING_MAIN_SOURCE =
    "import pkg.Deep;\nclass Main { static function main() { var here<caret> = 0; } }";
  private static final String OTHER_PACKAGE_MAIN_SOURCE =
    "package other;\nclass Main { static function main() { var here<caret> = 0; } }";

  @Override
  protected String getBasePath() {
    return "";
  }

  /**
   * The evaluate-window editor experience: a bare class reference typed in the
   * fragment resolves against the breakpoint file's imports (so it is not flagged
   * unresolved and no text-mutating "import?" fix is offered).
   */
  @Test
  @DisplayName("fragment resolves imported class against context")
  public void testFragmentResolvesImportedClassAgainstContext() {
    PsiFile fragment = deepFragment(IMPORTING_MAIN_SOURCE);

    PsiElement target = leftmostReference(fragment, "Deep").resolve();

    HaxeClass deep = assertInstanceOf(HaxeClass.class, target, "`Deep` resolves to a class via the inherited context imports");
    assertEquals("pkg.Deep", deep.getQualifiedName());
  }

  /**
   * The no-import baseline: a fragment whose context sits in another package
   * does NOT resolve the bare class name. The positive half — an import held
   * on the fragment resolving it without touching the text — is
   * {@link #testAddImportHelperHoldsImportOnFragmentInsteadOfText}.
   */
  @Test
  @DisplayName("bare fragment does not resolve an unimported class")
  public void testBareFragmentDoesNotResolveAnUnimportedClass() {
    PsiFile fragment = deepFragment(OTHER_PACKAGE_MAIN_SOURCE);

    assertNull(leftmostReference(fragment, "Deep").resolve(), "`Deep` is not resolvable from package `other` without an import");
  }

  /**
   * The auto-import chokepoint: every path that adds an import (completion,
   * copy/paste, reference binding, the add-import intentions) goes through
   * {@link HaxeAddImportHelper#addImport}. On a fragment it must hold the import
   * on the fragment and leave the evaluated text untouched — never insert an
   * {@code import} statement that would break evaluation (for fragments,
   * {@code addImport} is {@code importClass}).
   */
  @Test
  @DisplayName("add import helper holds import on fragment instead of text")
  public void testAddImportHelperHoldsImportOnFragmentInsteadOfText() {
    PsiFile fragment = deepFragment(OTHER_PACKAGE_MAIN_SOURCE);

    WriteCommandAction.runWriteCommandAction(getProject(), () -> { HaxeAddImportHelper.addImport("pkg.Deep", fragment); });

    assertEquals("Deep.marker", fragment.getText(), "the import must not enter the evaluated text");
    assertTrue(((HaxeExpressionCodeFragment)fragment).getImportedTypeNames().contains("pkg.Deep"), "the import is held on the fragment");
    HaxeClass deep = assertInstanceOf(HaxeClass.class, leftmostReference(fragment, "Deep").resolve(), "`Deep` now resolves");
    assertEquals("pkg.Deep", deep.getQualifiedName());
  }

  /** The `Deep.marker` fragment against the caret context {@code mainSource} gives in the Deep project. */
  private PsiFile deepFragment(String mainSource) {
    HaxeDebuggerTestFixtures.deepProject(myFixture, mainSource);
    return HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Deep.marker", contextAtCaret(), false);
  }

  private static HaxeReferenceExpression leftmostReference(PsiFile fragment, String name) {
    for (HaxeReferenceExpression ref : PsiTreeUtil.findChildrenOfType(fragment, HaxeReferenceExpression.class)) {
      if (ref.getQualifier() == null && name.equals(ref.getText())) {
        return ref;
      }
    }
    return null;
  }
}
