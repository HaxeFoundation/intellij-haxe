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

import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.refactoring.rename.HaxeRenameProcessor;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.util.ArrayUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeRenameTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/rename/";
  }

  public void doTest(String newName, String... additionalFiles) {
    myFixture.testRename(getSourceFileName(), getResultFileName(), newName, additionalFiles);
  }

  public void doTestOnNthSelection(int n, String newName, String... additionalFiles) {
    // One-based, for humans. :)
    assertTrue(n > 0);

    myFixture.configureByFiles(ArrayUtil.reverseArray(ArrayUtil.append(additionalFiles, getTruncatedSourceFileName())));

    // Extract the caret info, and then reset it to just the selection/position requested.
    List<CaretState> carets = myFixture.getEditor().getCaretModel().getCaretsAndSelections();
    assertNotEmpty(carets);  // No carets/selections in the source file.
    assertTrue(carets.size() >= n);

    List<CaretState> useCaret = new ArrayList<CaretState>(1);
    useCaret.add(carets.get(n - 1));
    myFixture.getEditor().getCaretModel().setCaretsAndSelections(useCaret);

    myFixture.testRename(getTruncatedResultFileName(), newName);
  }
  public void doTestWithoutFileVerify(String newName, int dialogAnswer, Map<String, String> expectedRename, String... additionalFiles) {
    doTest(newName,dialogAnswer,expectedRename, false, additionalFiles);
  }
  public void doTest(String newName, int dialogAnswer, Map<String, String> expectedRename,  String... additionalFiles) {
    doTest(newName,dialogAnswer,expectedRename, true, additionalFiles);
  }
  public void doTest(String newName, int dialogAnswer, Map<String, String> expectedRename, boolean verifyAfter,  String... additionalFiles) {
    HaxeRenameProcessor.alsoRenameAnswer = dialogAnswer;
    myFixture.configureByFiles(ArrayUtil.reverseArray(ArrayUtil.append(additionalFiles, getTruncatedSourceFileName())));


    PsiElement elementAtCaret = myFixture.getElementAtCaret();
    RenameProcessor renameProcessor = new RenameProcessor(elementAtCaret.getProject(), elementAtCaret, newName, true, true);


    LinkedHashMap<PsiElement, String> allRenames = new LinkedHashMap<>();
    allRenames.put(elementAtCaret, newName);
    renameProcessor.prepareRenaming(elementAtCaret, newName, allRenames);

    Map<@Nullable String, String> renameMap = allRenames.entrySet().stream()
            .collect(Collectors.toMap(HaxeRenameTest::getElementName, Map.Entry::getValue));

    renameProcessor.doRun();

    log.warn("----------");
    for (Map.Entry<String, String> entry : renameMap.entrySet()) {
      log.warn(String.format("rename entry '%s' => '%s'", entry.getKey(), entry.getValue()));
    }
    log.warn("----------");

    for (Map.Entry<String, String> entry : expectedRename.entrySet()) {
      String key = entry.getKey();
      assertTrue("no rename entry found for " + key, renameMap.containsKey(key));
      assertEquals(entry.getValue(), renameMap.get(key));
    }

    expectedRename.keySet().forEach(renameMap::remove);
    if(!expectedRename.isEmpty()){
      for (Map.Entry<String, String> entry : renameMap.entrySet()) {
        log.warn(String.format("unexpected rename entry '%s' => '%s'", entry.getKey(), entry.getValue()));
      }
    }
    assertEquals(0, renameMap.size());
    if(verifyAfter) {
      myFixture.checkResultByFile(getTruncatedResultFileName());
    }

  }

  private static @Nullable String getElementName(Map.Entry<PsiElement, String> e) {
    PsiElement psiElement = e.getKey();
    if(psiElement instanceof HaxeComponentName componentName) {
      psiElement = componentName.getParent();
    }
    if(psiElement instanceof PsiNamedElement element) {

      if(element instanceof PsiFile) {
        return "FILE:"+element.getName();
      }
      HaxeComponentType haxeComponentType = HaxeComponentType.typeOf(element);
      String name = haxeComponentType != null ? haxeComponentType.name() : "<unknown>";
      return name+":"+element.getName();
    }else {
      return null;
    }
  }

  private String toSourceName(String name) {
    return name + ".hx";
  }

  private String toResultName(String name) {
    return name + "After.hx";
  }

  private String getSourceFileName() {
    return toSourceName(getTestName(false));
  }

  private String getResultFileName() {
    return toResultName(getTestName(false));
  }

  private String getTruncatedSourceFileName() {
    return toSourceName(getTruncatedTestName());
  }

  private String getTruncatedResultFileName() {
    return toResultName(getTruncatedTestName());
  }

  private String getTruncatedTestName() {
    String testName = getTestName(false);

    int i = testName.length() - 1;
    for (; i > 0 && Character.isDigit(testName.charAt(i)); --i)
      ;

    return testName.substring(0, i + 1);
  }

  @Test
  public void testLocalVariable1() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testLocalVariable2() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testFunctionParameter() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testMethod() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testMainClass() throws Throwable {
    doTest("MainClassAfter");
  }

  @Test
  public void testStaticField() throws Throwable {
    doTest("fooNew", "additional/StaticFieldHelper.hx");
  }

  @Test
  public void testStaticMethod() throws Throwable {
    doTest("fooNew", "additional/StaticMethodHelper.hx");
  }

  @Test
  public void testCatchParameter() throws Throwable {
    doTest("error");
  }

  @Test
  public void testForVar() throws Throwable {
    doTest("index");
  }

  @Test
  public void testRenameGenericParam() throws Throwable {
    doTest("P");
  }

  @Test
  public void testDoNotRenameConstructor1() throws Throwable {
    doTestOnNthSelection(1, "After");
  }

  @Test
  public void testDoNotRenameConstructor2() throws Throwable {
    doTestOnNthSelection(2, "After");
  }

  @Test
  public void testDoNotRenameConstructor3() throws Throwable {
    doTestOnNthSelection(3, "After");
  }

  @Test
  public void testDoNotRenameConstructorName() throws Throwable {
    doTest("foo");
  }

  @Test
  public void testRenameModuleAndClass1() {
    Map<String, String> expectedRenames = Map.of(
            "FILE:RenameModuleAndClass.hx", "NewClassAndModuleName.hx",
            "MODULE:RenameModuleAndClass", "NewClassAndModuleName",
            "CLASS:RenameModuleAndClass", "NewClassAndModuleName"
    );
    doTest("NewClassAndModuleName", MessageConstants.YES, expectedRenames);
  }

  @Test
  public void testRenameModuleAndClass2() {
    Map<String, String> expectedRenames = Map.of(
            "CLASS:RenameModuleAndClass", "NewClassAndModuleName"
    );
    doTest("NewClassAndModuleName", MessageConstants.NO, expectedRenames);
  }

  @Test
  public void testRenameModuleAndClass3() {
    Map<String, String> expectedRenames = Map.of();
    doTestWithoutFileVerify("NewClassAndModuleName", MessageConstants.CANCEL, expectedRenames);
  }
}
