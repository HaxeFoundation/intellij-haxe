package com.intellij.plugins.haxe.runner.debugger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.util.ThreeState;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.impl.XDebuggerHistoryManager;

/**
 * The debugger's Set Value editor expects a plain VALUE: completion must not
 * auto-pop there, or the Enter that should submit the typed literal picks a
 * lookup suggestion instead: entering a number appends the
 * "function" keyword. The platform never tells the editors provider which
 * editor a fragment is for, but the Set Value editor's expressions live in
 * the "setValue" expression history — an expression found there identifies
 * the editor, and the provider tags its fragment. The confidence then
 * suppresses auto-popup for tagged fragments only; evaluate and watches
 * editors keep completion.
 */
@DisplayName("Debugger: set value completion suppression")
public class HaxeSetValueCompletionSuppressionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  private PsiFile createFragment(XExpression expression) {
    Document document = new HaxeDebuggerEditorsProvider().createDocument(
      getProject(), expression, null, EvaluationMode.EXPRESSION);
    PsiFile fragment = PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
    assertNotNull(fragment);
    return fragment;
  }

  private static XExpression expression(String text) {
    return XDebuggerUtil.getInstance().createExpression(text, HaxeLanguage.INSTANCE, null, EvaluationMode.EXPRESSION);
  }

  /** The confidence ignores the editor; any open one satisfies the signature. */
  private Editor someEditor() {
    myFixture.configureByText("Dummy.hx", "class Dummy {}");
    return myFixture.getEditor();
  }

  private ThreeState skipAutopopupFor(PsiFile fragment) {
    return new HaxeSetValueCompletionConfidence().shouldSkipAutopopup(someEditor(), fragment, fragment, 0);
  }

  /** A set-value expression (present in the "setValue" history) gets its fragment tagged. */
  @Test
  @DisplayName("set value fragment suppresses autopopup")
  public void testSetValueFragmentSuppressesAutopopup() {
    XExpression expression = expression("42");
    XDebuggerHistoryManager.getInstance(getProject()).addRecentExpression("setValue", expression);
    PsiFile fragment = createFragment(expression);
    assertEquals(ThreeState.YES, skipAutopopupFor(fragment));
  }

  /** An evaluate/watches expression (not in that history) keeps completion. */
  @Test
  @DisplayName("evaluate fragment keeps autopopup")
  public void testEvaluateFragmentKeepsAutopopup() {
    XDebuggerHistoryManager.getInstance(getProject()).addRecentExpression("setValue", expression("42"));
    PsiFile fragment = createFragment(expression("someIdentifier"));
    assertEquals(ThreeState.UNSURE, skipAutopopupFor(fragment));
  }
}
