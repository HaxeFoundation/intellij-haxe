/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.PsiElementUsageTarget;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageTargetUtil;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;

import java.util.*;

import static com.intellij.plugins.haxe.ide.HaxeFindUsagesHandlerFactory.TestInterface.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFindUsagesTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/findUsages/";
  }

  protected String getResultsPath() {
    return "results/" + getTestName(false) + ".txt";
  }

  protected void doTest(int size) throws Throwable {
    final Collection<UsageInfo> elements = findUsages();
    assertNotNull(elements);
    assertEquals(size, elements.size());
  }

  private Collection<UsageInfo> findUsages()
    throws Throwable {
    final UsageTarget[] targets = UsageTargetUtil.findUsageTargets(new DataProvider() {
      @Override
      public Object getData(@NonNls String dataId) {
        return ((EditorEx)myFixture.getEditor()).getDataContext().getData(dataId);
      }
    });

    assert targets != null && targets.length > 0 && targets[0] instanceof PsiElementUsageTarget;
    return myFixture.findUsages(((PsiElementUsageTarget)targets[0]).getElement());
  }

  public void testProperties1() throws Throwable {
    myFixture.configureByFiles("Properties1.hx");
    doTest(1);
  }

  public void testProperties2() throws Throwable {
    myFixture.configureByFiles("Properties2.hx");
    doTest(1);
  }

  public void testVarDeclaration() throws Throwable {
    myFixture.configureByFiles("VarDeclaration.hx", "com/bar/Foo.hx");
    doTest(0);
  }

  public void testLocalFunctionParameter() throws Throwable {
    myFixture.configureByFiles("LocalFunctionParameter.hx", "com/bar/Foo.hx");
    doTest(3);
  }

  public void testForDeclaration() throws Throwable {
    myFixture.configureByFiles("ForDeclaration.hx", "com/bar/IBar.hx");
    doTest(3);
  }

  public void testLocalVarDeclaration1() throws Throwable {
    myFixture.configureByFiles("LocalVarDeclaration1.hx");
    doTest(1);
  }

  public void testLocalVarDeclaration2() throws Throwable {
    myFixture.configureByFiles("LocalVarDeclaration2.hx");
    doTest(1);
  }

  public void testFunctionParameter() throws Throwable {
    myFixture.configureByFiles("FunctionParameter.hx");
    doTest(2);
  }

  public void testClassDeclaration() throws Throwable {
    myFixture.configureByFiles("com/bar/ClassToFind.hx", "ClassDeclaration.hx");
    doTest(7);
  }

  public void testClassConstructor() throws Throwable {
    myFixture.configureByFiles("ClassConstructor.hx");
    doTest(3);
  }


  //
  // Overrides tests
  //

  // Shortcut answers to the popup dialog when a superclass contains the same method.
  public static final TestDialog GET_BASE_CLASS = new TestDialog() {
    public int show(String message) { return getOptionIndex(getBaseClassOption()); }
  };
  public static final TestDialog GET_CURRENT_CLASS = new TestDialog() {
    public int show(String message) { return getOptionIndex(getCurrentClassOption()); }
  };
  public static final TestDialog GET_ANCESTOR_CLASSES = new TestDialog() {
    public int show(String message) { return getOptionIndex(getAncestorClassesOption()); }
  };


  public void doOverrideTest(String testFile, TestDialog answer) throws Throwable {
    TestDialogManager.setTestDialog(answer);
    myFixture.configureByFiles(testFile, "com/bar/TestSearchOverrides.hx");
    myFixture.copyFileToProject(getResultsPath());
    compareExpectedUsages(findUsages());
  }

  public void compareExpectedUsages(Collection<UsageInfo> foundUsages) {

    assertNotNull(foundUsages);

    // Need to keep the ordering constant, so sort the output.
    String[] formattedUsages = new String[foundUsages.size()];
    int i = 0;
    for (UsageInfo usage : foundUsages) {
      formattedUsages[i++] = prettyUsageMessage(usage);
    }
    Arrays.sort(formattedUsages, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });

    StringBuilder builder = new StringBuilder();
    for (String s : formattedUsages) {
      builder.append(s);
      builder.append('\n');
    }

    // Convert the output to a file.  MUST use an API that sets eventSystemEnabled==true, or the file won't open.
    PsiFile foundFile = PsiFileFactory.getInstance(myFixture.getProject())
      .createFileFromText("testResult", FileTypes.PLAIN_TEXT, builder.toString(),
                          LocalTimeCounter.currentTime(), true);
    VirtualFile vFile = foundFile.getViewProvider().getVirtualFile();

    myFixture.openFileInEditor(vFile); // Can't use foundFile.getVirtualFile(); it returns null.
    myFixture.checkResultByFile(getResultsPath(), false);
  }

  public String prettyUsageMessage(UsageInfo usage) {

    UsageInfo2UsageAdapter adapter = new UsageInfo2UsageAdapter(usage);
    StringBuilder builder = new StringBuilder();

    VirtualFile vFile = adapter.getFile();
    builder.append(null != vFile ? vFile.getName() : "<unknown file>");
    builder.append(", line ");
    builder.append(adapter.getLine() + 1);
    builder.append(':');
    builder.append(adapter.getPresentation().getPlainText());

    String tooltip = adapter.getPresentation().getTooltipText();
    if (null != tooltip) {
      builder.append(" {");
      builder.append(tooltip);
      builder.append("} ");
    }

    return builder.toString();
  }


  public void testFindCurrentClass() throws Throwable {
    doOverrideTest( "OverrideTop.hx", GET_CURRENT_CLASS);
  }

  public void testFindBaseClass() throws Throwable {
    doOverrideTest("OverrideTop.hx", GET_BASE_CLASS);
  }

  public void testFindAncestorClass() throws Throwable {
    doOverrideTest("OverrideTop.hx", GET_ANCESTOR_CLASSES);
  }


}
