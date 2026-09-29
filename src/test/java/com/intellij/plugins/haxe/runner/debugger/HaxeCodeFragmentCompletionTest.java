package com.intellij.plugins.haxe.runner.debugger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.intellij.testFramework.UsefulTestCase.assertContainsElements;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
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
@DisplayName("Debugger: code fragment completion")
public class HaxeCodeFragmentCompletionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The caret context of {@link HaxeDebuggerTestFixtures#instanceFrameProject}'s breakpoint. */
  private PsiElement frameContext() {
    HaxeDebuggerTestFixtures.instanceFrameProject(myFixture);
    return contextAtCaret();
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

  @Test
  @DisplayName("this completion lists own and inherited members")
  public void testThisCompletionListsOwnAndInheritedMembers() {
    assertContainsElements(fragmentCompletions("this."), "count", "update", "inherited", "baseAction");
  }

  @Test
  @DisplayName("super completion lists super class members")
  public void testSuperCompletionListsSuperClassMembers() {
    assertContainsElements(fragmentCompletions("super."), "inherited", "baseAction");
  }
}
