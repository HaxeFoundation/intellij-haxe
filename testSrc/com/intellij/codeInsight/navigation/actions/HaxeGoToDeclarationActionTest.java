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

  protected void doTest(PsiFile[] files, int expectedSize) {
    final PsiFile myFile = files[0];
    final Collection<PsiElement> elements = GotoDeclarationAction.suggestCandidates(myFile.findReferenceAt(myFixture.getCaretOffset()));
    assertNotNull(elements);
    assertEquals(expectedSize, elements.size());
  }

  public void testVarDeclaration() {
    doTest(myFixture.configureByFiles("VarDeclaration.hx", "com/bar/Foo.hx"), 1);
  }

  public void testFunctionParameter() {
    doTest(myFixture.configureByFiles("FunctionParameter.hx", "com/bar/Foo.hx"), 1);
  }

  public void testInterfaceParameter() {
    doTest(myFixture.configureByFiles("InterfaceDeclaration.hx", "com/bar/IBar.hx"), 1);
  }

  public void testForDeclaration() {
    doTest(myFixture.configureByFiles("ForDeclaration.hx"), 1);
  }

  public void testLocalVarDeclaration1() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration1.hx"), 1);
  }

  public void testLocalVarDeclaration2() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration2.hx"), 1);
  }

  public void testFunctionParameter1() {
    doTest(myFixture.configureByFiles("FunctionParameter1.hx"), 1);
  }

  public void testFunctionParameter2() {
    doTest(myFixture.configureByFiles("FunctionParameter2.hx"), 1);
  }

  public void testReference() {
    doTest(myFixture.configureByFiles("Reference.hx"), 0);
  }

  public void testSamePackage() {
    doTest(myFixture.configureByFiles("com/bar/Baz.hx", "com/bar/Foo.hx"), 1);
  }
}
