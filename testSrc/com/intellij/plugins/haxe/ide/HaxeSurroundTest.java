package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.surroundWith.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSurroundTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/surroundWith/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setTestStyleSettings();
  }

  private void setTestStyleSettings() {
    Project project = getProject();
    CodeStyleSettings currSettings = CodeStyleSettingsManager.getSettings(project);
    assertNotNull(currSettings);
    CodeStyleSettings tempSettings = currSettings.clone();
    CodeStyleSettings.IndentOptions indentOptions = tempSettings.getIndentOptions(HaxeFileType.HAXE_FILE_TYPE);
    indentOptions.INDENT_SIZE = 2;
    assertNotNull(indentOptions);
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(tempSettings);
  }

  protected void doTest(final Surrounder surrounder) throws Exception {
    myFixture.configureByFile(getTestName(false) + ".hx");

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        SurroundWithHandler.invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), surrounder);
        PsiDocumentManager.getInstance(getProject()).doPostponedOperationsAndUnblockDocument(myFixture.getDocument(myFixture.getFile()));
        CodeStyleManager.getInstance(myFixture.getProject()).reformat(myFixture.getFile());
      }
    });

    myFixture.checkResultByFile(getTestName(false) + "_after.hx");
  }

  public void testIf() throws Exception {
    doTest(new HaxeIfSurrounder());
  }

  public void testIfElse() throws Exception {
    doTest(new HaxeIfElseSurrounder());
  }

  public void testWhile() throws Exception {
    doTest(new HaxeWhileSurrounder());
  }

  public void testDoWhile() throws Exception {
    doTest(new HaxeDoWhileSurrounder());
  }

  public void testTryCatch() throws Exception {
    doTest(new HaxeTryCatchSurrounder());
  }
}
