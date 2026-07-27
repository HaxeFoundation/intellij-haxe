package com.intellij.plugins.haxe.runner.debugger.hashlink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionCodeFragment;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * The eager name-qualification linchpin: {@link HashLinkExpressionQualifier}
 * rewrites a bare class reference in an evaluate expression to its
 * fully-qualified name using the breakpoint file's imports/scope, so the adapter
 * — which resolves class-qualified statics only by FQN — accepts {@code
 * Deep.marker} without the user typing {@code pkg.Deep.marker}. Rewriting eagerly
 * (before sending) is what keeps a side-effecting sub-expression from being
 * re-evaluated on a lazy retry.
 */
public class HashLinkExpressionQualifierTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** A `class Deep` in package `pkg`, reachable via `import pkg.Deep`. */
  private PsiElement importingContext() {
    myFixture.addFileToProject("pkg/Deep.hx",
                               "package pkg;\nclass Deep { public static var marker:Int = 99; }");
    myFixture.configureByText("Main.hx",
                              "import pkg.Deep;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");
    return context;
  }

  @Test
  public void testImportedClassIsQualified() {
    PsiElement context = importingContext();
    assertEquals("pkg.Deep.marker", HashLinkExpressionQualifier.rewrite(getProject(), context, "Deep.marker"));
  }

  @Test
  public void testAlreadyQualifiedIsUnchanged() {
    PsiElement context = importingContext();
    assertEquals("pkg.Deep.marker", HashLinkExpressionQualifier.rewrite(getProject(), context, "pkg.Deep.marker"));
  }

  @Test
  public void testNonClassLeftmostIsUnchanged() {
    PsiElement context = importingContext();
    // `here` is a local, not a class — leave it for the adapter's frame-local
    // resolution; only genuine class names are qualified
    assertEquals("here.field", HashLinkExpressionQualifier.rewrite(getProject(), context, "here.field"));
  }

  /**
   * The reason for eager rewriting: a compound expression is sent ONCE with only
   * the class name qualified, so a side-effecting sub-expression like {@code
   * increase()} is never re-evaluated by a lazy "resolve-then-retry".
   */
  @Test
  public void testCompoundExpressionQualifiesOnlyTheClass() {
    PsiElement context = importingContext();
    assertEquals("increase() + pkg.Deep.marker",
                 HashLinkExpressionQualifier.rewrite(getProject(), context, "increase() + Deep.marker"));
  }

  /**
   * The evaluate-window editor experience: a bare class reference typed in the
   * fragment resolves against the breakpoint file's imports (so it is not flagged
   * unresolved and no text-mutating "import?" fix is offered). Before the fragment
   * inherited its context's imports, {@code Deep.resolve()} returned {@code null}.
   */
  @Test
  public void testFragmentResolvesImportedClassAgainstContext() {
    PsiElement context = importingContext();
    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Deep.marker", context, false);

    HaxeReferenceExpression deep = leftmostReference(fragment, "Deep");
    assertNotNull(deep, "the `Deep` reference is present in the parsed fragment");

    PsiElement target = deep.resolve();
    assertTrue(target instanceof HaxeClass, "`Deep` resolves to a class via the inherited context imports, got "
               + (target == null ? "null" : target.getClass().getSimpleName()));
    assertEquals("pkg.Deep", ((HaxeClass)target).getQualifiedName());
  }

  /**
   * Part B — the Java-parity import-holder: a class the breakpoint file does NOT
   * import (here the fragment's context is in package {@code other}) resolves once
   * it is added via {@link HaxeExpressionCodeFragment#importClass}, and the import
   * never enters the fragment's (evaluated) text. Fresh fragments avoid a poisoned
   * resolve cache — the same reason the daemon must re-resolve after an import.
   */
  @Test
  public void testFragmentStoredImportResolvesWithoutTouchingText() {
    myFixture.addFileToProject("pkg/Deep.hx",
                               "package pkg;\nclass Deep { public static var marker:Int = 99; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");

    PsiFile bare = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Deep.marker", context, false);
    assertNull(leftmostReference(bare, "Deep").resolve(), "`Deep` is not resolvable from package `other` without an import");

    PsiFile imported = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Deep.marker", context, false);
    ((HaxeExpressionCodeFragment)imported).importClass("pkg.Deep");
    PsiElement target = leftmostReference(imported, "Deep").resolve();
    assertTrue(target instanceof HaxeClass, "`Deep` resolves after importClass, got " + (target == null ? "null" : target.getClass().getSimpleName()));
    assertEquals("pkg.Deep", ((HaxeClass)target).getQualifiedName());
    assertEquals("Deep.marker", imported.getText(), "the import stays out of the evaluated text");
  }

  /**
   * Part C — project-wide fallback: a class the breakpoint file neither imports
   * nor shares a package with is still qualified when it is the project's only
   * class of that short name, so it evaluates without the user adding an import.
   */
  @Test
  public void testUnreachableUniqueClassQualifiedByProjectFallback() {
    myFixture.addFileToProject("far/Widget.hx",
                               "package far;\nclass Widget { public static var count:Int = 3; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");

    assertEquals("far.Widget.count", HashLinkExpressionQualifier.rewrite(getProject(), context, "Widget.count"));
  }

  /** Two classes share the short name — ambiguous, so the qualifier leaves it bare. */
  @Test
  public void testAmbiguousClassNameIsLeftBare() {
    myFixture.addFileToProject("a/Widget.hx",
                               "package a;\nclass Widget { public static var count:Int = 1; }");
    myFixture.addFileToProject("b/Widget.hx",
                               "package b;\nclass Widget { public static var count:Int = 2; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");

    assertEquals("Widget.count", HashLinkExpressionQualifier.rewrite(getProject(), context, "Widget.count"));
  }

  /**
   * The auto-import chokepoint: every path that adds an import (completion,
   * copy/paste, reference binding, the add-import intentions) goes through
   * {@link HaxeAddImportHelper#addImport}. On a fragment it must hold the import
   * on the fragment and leave the evaluated text untouched — never insert an
   * {@code import} statement that would break evaluation.
   */
  @Test
  public void testAddImportHelperHoldsImportOnFragmentInsteadOfText() {
    myFixture.addFileToProject("far/Widget.hx",
                               "package far;\nclass Widget { public static var count:Int = 3; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");

    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Widget.count", context, false);
    WriteCommandAction.runWriteCommandAction(getProject(),
                                             () -> { HaxeAddImportHelper.addImport("far.Widget", fragment); });

    assertEquals("Widget.count", fragment.getText(), "the import must not enter the evaluated text");
    assertTrue(((HaxeExpressionCodeFragment)fragment).getImportedTypeNames().contains("far.Widget"), "the import is held on the fragment");
    PsiElement target = leftmostReference(fragment, "Widget").resolve();
    assertTrue(target instanceof HaxeClass, "`Widget` now resolves");
    assertEquals("far.Widget", ((HaxeClass)target).getQualifiedName());
  }

  private static HaxeReferenceExpression leftmostReference(PsiFile fragment, String name) {
    for (HaxeReferenceExpression ref : PsiTreeUtil.findChildrenOfType(fragment, HaxeReferenceExpression.class)) {
      if (ref.getQualifier() == null && name.equals(ref.getText())) {
        return ref;
      }
    }
    return null;
  }

  @Test
  public void testSamePackageClassNeedsNoImport() {
    myFixture.addFileToProject("pkg/Deep.hx",
                               "package pkg;\nclass Deep { public static var marker:Int = 99; }");
    myFixture.configureByText("Main.hx",
                              "package pkg;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(context, "context element at the breakpoint");
    assertEquals("pkg.Deep.marker", HashLinkExpressionQualifier.rewrite(getProject(), context, "Deep.marker"));
  }
}
