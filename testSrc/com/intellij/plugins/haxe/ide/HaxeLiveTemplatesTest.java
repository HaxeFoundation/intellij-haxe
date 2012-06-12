package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.impl.actions.ListTemplatesAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLiveTemplatesTest extends LightCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return FileUtil.toSystemDependentName("/plugins/haxe/testData/liveTemplates/");
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

  public static void expandTemplate(final Editor editor) {
    new ListTemplatesAction().actionPerformedImpl(editor.getProject(), editor);
    ((LookupImpl)LookupManager.getActiveLookup(editor)).finishLookup(Lookup.NORMAL_SELECT_CHAR);
  }

  private void doTest() throws Exception {
    doTest(getTestName(false) + ".hx");
  }

  private void doTest(String... files) throws Exception {
    myFixture.configureByFiles(files);
    expandTemplate(myFixture.getEditor());
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CodeStyleManager.getInstance(myFixture.getProject()).reformat(myFixture.getFile());
      }
    });
    myFixture.getEditor().getSelectionModel().removeSelection();
    myFixture.checkResultByFile(getTestName(false) + "_after.hx");
  }

  public void testIter() throws Throwable {
    doTest("Iter.hx", "Array.hx");
  }

  public void testItar() throws Throwable {
    doTest("Itar.hx", "Array.hx");
  }
}
