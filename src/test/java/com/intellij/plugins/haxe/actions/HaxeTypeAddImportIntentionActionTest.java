/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.actions;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeAddImportIntentionActionTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/addImportIntention/";
  }

  protected CommonCodeStyleSettings myTestStyleSettings;


  public void doTest() {
    final PsiFile file = PsiDocumentManager.getInstance(myFixture.getProject()).getPsiFile(myFixture.getEditor().getDocument());
    assertNotNull(file);
    final HaxeType type = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.getCaretOffset()), HaxeType.class, false);
    assertNotNull(type);
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(type);
    new HaxeTypeAddImportIntentionAction(type, new java.util.ArrayList<HaxeComponent>(HaxeClassNameStubIndex
      .getByNameFiltered(type.getReferenceExpression().getText(), type.getProject(), scope)))
      .execute();
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(false) + ".txt");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setTestStyleSettings();
  }


  @Override
  public void setTestStyleSettings() {
    Project project = getProject();
    CodeStyleSettings currSettings = CodeStyleSettingsManager.getSettings(project);
    assertNotNull(currSettings);
    CodeStyleSettings tempSettings = currSettings.clone();
    CodeStyleSettings.IndentOptions indentOptions = tempSettings.getIndentOptions(HaxeFileType.INSTANCE);
    assertNotNull(indentOptions);
    defineStyleSettings(tempSettings);
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(tempSettings);
  }

  protected void defineStyleSettings(CodeStyleSettings tempSettings) {
    myTestStyleSettings = tempSettings.getCommonSettings(HaxeLanguage.INSTANCE);
    myTestStyleSettings.KEEP_BLANK_LINES_IN_CODE = 2;
    myTestStyleSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    myTestStyleSettings.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    myTestStyleSettings.ALIGN_MULTILINE_PARAMETERS = false;
    myTestStyleSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false;
    myTestStyleSettings.KEEP_FIRST_COLUMN_COMMENT = false;
    myTestStyleSettings.BLANK_LINES_AFTER_PACKAGE = 2;
    myTestStyleSettings.BLANK_LINES_AFTER_IMPORTS = 2;
  }


  @Test
  public void testSimple() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Bar.hx");
    doTest();
  }

  @Test
  public void testHelper() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Bar.hx");
    doTest();
  }

  @Test
  public void testModule() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "foo/Types.hx");
    doTest();
  }
}
