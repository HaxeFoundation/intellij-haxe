package com.intellij.plugins.haxe.runner.debugger.hashlink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerTestFixtures;
import com.intellij.psi.PsiElement;

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

  /** The caret context of {@code mainSource} in the Deep-declaring project. */
  private PsiElement contextIn(String mainSource) {
    HaxeDebuggerTestFixtures.deepProject(myFixture, mainSource);
    return contextAtCaret();
  }

  /** The Deep-declaring project with a main that reaches it via `import pkg.Deep`. */
  private PsiElement importingContext() {
    return contextIn("import pkg.Deep;\nclass Main { static function main() { var here<caret> = 0; } }");
  }
}
