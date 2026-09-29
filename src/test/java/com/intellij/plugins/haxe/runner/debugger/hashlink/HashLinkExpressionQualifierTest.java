package com.intellij.plugins.haxe.runner.debugger.hashlink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

import java.util.List;

/**
 * The eager name-qualification linchpin: {@link HashLinkExpressionQualifier}
 * rewrites a bare class reference in an evaluate expression to its
 * fully-qualified name using the breakpoint file's imports/scope, so the adapter
 * — which resolves class-qualified statics only by FQN — accepts {@code
 * Deep.marker} without the user typing {@code pkg.Deep.marker}. Rewriting eagerly
 * (before sending) is what keeps a side-effecting sub-expression from being
 * re-evaluated on a lazy retry.
 */
@DisplayName("Debugger: hashlink expression qualifier")
public class HashLinkExpressionQualifierTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /**
   * (evaluate expression, its eager rewrite) against the importing context.
   * `here` is a local, not a class — it stays for the adapter's frame-local
   * resolution; a compound expression is sent ONCE with only the class name
   * qualified, so a side-effecting sub-expression like {@code increase()} is
   * never re-evaluated by a lazy "resolve-then-retry".
   */
  static final List<Arguments> IMPORTING_CONTEXT_REWRITES = List.of(
    arguments("Deep.marker", "pkg.Deep.marker"),
    arguments("pkg.Deep.marker", "pkg.Deep.marker"),
    arguments("here.field", "here.field"),
    arguments("increase() + Deep.marker", "increase() + pkg.Deep.marker"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("IMPORTING_CONTEXT_REWRITES")
  @DisplayName("rewrites against the importing context")
  public void testRewritesAgainstTheImportingContext(String expression, String expected) {
    assertEquals(expected, HashLinkExpressionQualifier.rewrite(getProject(), importingContext(), expression));
  }

  @Test
  @DisplayName("same package class needs no import")
  public void testSamePackageClassNeedsNoImport() {
    PsiElement context = contextIn("package pkg;\nclass Main { static function main() { var here<caret> = 0; } }");
    assertEquals("pkg.Deep.marker", HashLinkExpressionQualifier.rewrite(getProject(), context, "Deep.marker"));
  }

  /**
   * The evaluate-window editor experience: a bare class reference typed in the
   * fragment resolves against the breakpoint file's imports (so it is not flagged
   * unresolved and no text-mutating "import?" fix is offered). Before the fragment
   * inherited its context's imports, {@code Deep.resolve()} returned {@code null}.
   */
  @Test
  @DisplayName("fragment resolves imported class against context")
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
   * The no-import baseline: a fragment whose context sits in another package
   * does NOT resolve the bare class name. The positive half — an import held
   * on the fragment resolving it without touching the text — is pinned by
   * {@link #testAddImportHelperHoldsImportOnFragmentInsteadOfText} (for
   * fragments, {@code HaxeAddImportHelper.addImport} is {@code importClass}).
   */
  @Test
  @DisplayName("bare fragment does not resolve an unimported class")
  public void testBareFragmentDoesNotResolveAnUnimportedClass() {
    PsiElement context = contextIn("package other;\nclass Main { static function main() { var here<caret> = 0; } }");

    PsiFile bare = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Deep.marker", context, false);
    assertNull(leftmostReference(bare, "Deep").resolve(), "`Deep` is not resolvable from package `other` without an import");
  }

  /**
   * Part C — project-wide fallback: a class the breakpoint file neither imports
   * nor shares a package with is still qualified when it is the project's only
   * class of that short name, so it evaluates without the user adding an import.
   */
  @Test
  @DisplayName("unreachable unique class qualified by project fallback")
  public void testUnreachableUniqueClassQualifiedByProjectFallback() {
    myFixture.addFileToProject("far/Widget.hx",
                               "package far;\nclass Widget { public static var count:Int = 3; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = contextAtCaret();

    assertEquals("far.Widget.count", HashLinkExpressionQualifier.rewrite(getProject(), context, "Widget.count"));
  }

  /** Two classes share the short name — ambiguous, so the qualifier leaves it bare. */
  @Test
  @DisplayName("ambiguous class name is left bare")
  public void testAmbiguousClassNameIsLeftBare() {
    myFixture.addFileToProject("a/Widget.hx",
                               "package a;\nclass Widget { public static var count:Int = 1; }");
    myFixture.addFileToProject("b/Widget.hx",
                               "package b;\nclass Widget { public static var count:Int = 2; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = contextAtCaret();

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
  @DisplayName("add import helper holds import on fragment instead of text")
  public void testAddImportHelperHoldsImportOnFragmentInsteadOfText() {
    myFixture.addFileToProject("far/Widget.hx",
                               "package far;\nclass Widget { public static var count:Int = 3; }");
    myFixture.configureByText("Main.hx",
                              "package other;\nclass Main { static function main() { var here<caret> = 0; } }");
    PsiElement context = contextAtCaret();

    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(getProject(), "Widget.count", context, false);
    WriteCommandAction.runWriteCommandAction(getProject(),
                                             () -> { HaxeAddImportHelper.addImport("far.Widget", fragment); });

    assertEquals("Widget.count", fragment.getText(), "the import must not enter the evaluated text");
    assertTrue(((HaxeExpressionCodeFragment)fragment).getImportedTypeNames().contains("far.Widget"), "the import is held on the fragment");
    PsiElement target = leftmostReference(fragment, "Widget").resolve();
    assertTrue(target instanceof HaxeClass, "`Widget` now resolves");
    assertEquals("far.Widget", ((HaxeClass)target).getQualifiedName());
  }

  /** A `class Deep` in package `pkg`; the caret context comes from {@code mainSource}. */
  private PsiElement contextIn(String mainSource) {
    myFixture.addFileToProject("pkg/Deep.hx",
                               "package pkg;\nclass Deep { public static var marker:Int = 99; }");
    myFixture.configureByText("Main.hx", mainSource);
    return contextAtCaret();
  }

  /** The Deep-declaring project with a main that reaches it via `import pkg.Deep`. */
  private PsiElement importingContext() {
    return contextIn("import pkg.Deep;\nclass Main { static function main() { var here<caret> = 0; } }");
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
