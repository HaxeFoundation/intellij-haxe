package com.intellij.codeInsight.navigation.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToDeclarationActionTest extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/goto/");
  }

  protected void doTest(PsiFile[] files) {
    final PsiFile myFile = files[0];
    final Collection<PsiElement> elements = GotoDeclarationAction.suggestCandidates(myFile.findReferenceAt(myFixture.getCaretOffset()));
    assertNotNull(elements);
    assertEquals(1, elements.size());
  }

  public void testVarDeclaration() {
    doTest(myFixture.configureByFiles("VarDeclaration.hx", "com/bar/Foo.hx"));
  }

  public void testFunctionParametr() {
    doTest(myFixture.configureByFiles("FunctionParametr.hx", "com/bar/Foo.hx"));
  }

  public void testLocalVarDeclaration1() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration1.hx"));
  }

  public void testLocalVarDeclaration2() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration2.hx"));
  }

  public void testFunctionParameter1() {
    doTest(myFixture.configureByFiles("FunctionParameter1.hx"));
  }

  public void testFunctionParameter2() {
    doTest(myFixture.configureByFiles("FunctionParameter2.hx"));
  }
}
