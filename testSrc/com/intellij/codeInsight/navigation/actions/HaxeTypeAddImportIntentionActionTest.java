package com.intellij.codeInsight.navigation.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeAddImportIntentionActionTest extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/addImportIntention/");
  }

  public void doTest() {
    final PsiFile file = PsiDocumentManager.getInstance(myFixture.getProject()).getPsiFile(myFixture.getEditor().getDocument());
    assertNotNull(file);
    final HaxeType type = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.getCaretOffset()), HaxeType.class, false);
    assertNotNull(type);
    new HaxeTypeAddImportIntentionAction(type, HaxeComponentIndex.getItemsByName(type.getExpression().getText(), type.getProject()))
      .execute();
    myFixture.checkResultByFile(getTestName(false) + ".txt");
  }

  public void testSimple() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Bar.hx");
    doTest();
  }
}
