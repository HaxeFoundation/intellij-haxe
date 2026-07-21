package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.List;

/**
 * Completion inside the debugger's Evaluate Expression / Watches editor — a
 * detached {@code HaxeExpressionCodeFragment} whose only tie to the stopped
 * frame is its creation context. {@code this.} and {@code super.} member
 * suggestions come from the expression evaluator's type of the keyword, and
 * both typings need the ENCLOSING class, which a fragment only exposes
 * through {@code getContext()}: {@code this} via UsefulPsiTreeUtil.getAncestor
 * (which crosses the fragment boundary), {@code super} via the enclosing
 * class's extends list in handleSuperExpression. Without the boundary
 * crossing both typed as Dynamic/unknown and the popup came up empty.
 */
public class HaxeCodeFragmentCompletionTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** Instance frame: stopped inside Widget.update(), where `this` is a Widget extends Base. */
  private PsiElement frameContext() {
    myFixture.addFileToProject("Base.hx",
                               "class Base { public var inherited:Int = 2; public function baseAction():Void {} }");
    myFixture.configureByText("Widget.hx",
                              "class Widget extends Base { var count:Int = 1;\n"
                              + "  function update() { trace<caret>(count); }\n"
                              + "  static function main() { new Widget().update(); } }");
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull("context element at the breakpoint", context);
    return context;
  }

  /** Completion lookup strings at the end of {@code text} typed into an evaluate fragment. */
  private List<String> fragmentCompletions(String text) {
    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(getProject(), text, frameContext(), true);
    myFixture.configureFromExistingVirtualFile(fragment.getVirtualFile());
    myFixture.getEditor().getCaretModel().moveToOffset(text.length());
    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    return lookups != null ? lookups : List.of();
  }

  public void testThisCompletionListsOwnMembers() {
    assertContainsElements(fragmentCompletions("this."), "count", "update");
  }

  public void testThisCompletionListsInheritedMembers() {
    assertContainsElements(fragmentCompletions("this."), "inherited", "baseAction");
  }

  public void testSuperCompletionListsSuperClassMembers() {
    assertContainsElements(fragmentCompletions("super."), "inherited", "baseAction");
  }
}
