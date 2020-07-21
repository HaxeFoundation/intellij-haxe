/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToDeclarationActionTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/goto/";
  }

  protected void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
  }

  protected void doTest(PsiFile file, int expectedSize) {
    doTest(new PsiFile[]{file}, expectedSize);
  }

  protected void doTest(PsiFile[] files, int expectedSize) {
    assertNotNull(files);
    final PsiFile myFile = files[0];
    assertNotNull(myFile);
    final TargetElementUtil util = TargetElementUtil.getInstance();
    assertNotNull(util);

    final PsiReference found = myFile.findReferenceAt(myFixture.getCaretOffset());
    assertNotNull(found);
    final Collection<PsiElement> elements = util.getTargetCandidates(found);
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

  public void testForDeclaration1() {
    doTest(myFixture.configureByFiles("ForDeclaration1.hx"), 1);
  }

  public void testForDeclaration2() {
    doTest(myFixture.configureByFiles("ForDeclaration2.hx"), 1);
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

  public void testFunctionParameter3() {
    doTest(myFixture.configureByFiles("FunctionParameter3.hx"), 1);
  }

  public void testReference() {
    doTest(myFixture.configureByFiles("Reference.hx"), 1);
  }

  public void testThisExpression() {
    doTest(myFixture.configureByFiles("ThisExpression.hx"), 1);
  }

  public void testThisShadowing() {
    doTest(myFixture.configureByFiles("ThisShadowing.hx"), 0);
  }

  public void testStaticClassMember1() {
    doTest(myFixture.configureByFiles("StaticClassMember1.hx", "com/bar/Foo.hx"), 1);
  }

  public void testStaticClassMember2() {
    doTest(myFixture.configureByFiles("StaticClassMember2.hx", "com/bar/Foo.hx"), 1);
  }

  public void testFunctionCall() {
    doTest(myFixture.configureByFiles("FunctionCall.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testUsingUtil1() {
    doTest(myFixture.configureByFiles("UsingUtil1.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testUsingUtil2() {
    doTest(myFixture.configureByFiles("UsingUtil2.hx", "com/utils/MathUtil.hx"), 0);
  }

  public void testUsingUtil3() {
    doTest(myFixture.configureByFiles("UsingUtil3.hx", "com/utils/StringUtil.hx", "com/utils/Tools.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testSamePackage() {
    doTest(myFixture.configureByFiles("com/bar/Baz.hx", "com/bar/Foo.hx"), 1);
  }

  public void testExternClass1() {
    doTest(myFixture.configureByFiles("ExternClass1.hx", "com/bar/Foo.hx"), 1);
  }

  public void testExternClass2() {
    doTest(myFixture.configureByFiles("ExternClass2.hx"), 1);
  }

  public void testSuperField() {
    doTest(myFixture.configureByFiles("SuperField.hx", "com/bar/Foo.hx"), 1);
  }

  public void testReferenceExpression1() {
    doTest(myFixture.configureByFiles("ReferenceExpression1.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  public void testReferenceExpression2() {
    doTest(myFixture.configureByFiles("ReferenceExpression2.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  public void testReferenceExpression3() {
    doTest(myFixture.configureByFiles("ReferenceExpression3.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  public void testReferenceExpression4() {
    doTest(myFixture.configureByFiles("ReferenceExpression4.hx",
                                      "com/bar/Foo.hx",
                                      "com/bar/Baz.hx",
                                      "com/bar/IBar.hx",
                                      "com/bar/SuperClass.hx"), 1);
  }

  public void testReferenceExpression5() {
    doTest(myFixture.configureByFiles("ReferenceExpression5.hx", "com/bar/Foo.hx"), 1);
  }

  public void testReferenceExpression6() {
    doTest(myFixture.configureByFiles("ReferenceExpression6.hx"), 0);
  }

  public void testReferenceExpression7() {
    doTest(myFixture.configureByFiles("ReferenceExpression7.hx"), 0);
  }

  public void testReferenceExpression8() {
    doTest(myFixture.configureByFiles("ReferenceExpression8.hx"), 0);
  }

  public void testReferenceExpression9() {
    doTest(myFixture.configureByFiles("ReferenceExpression9.hx"), 0);
  }

  public void testReferenceExpression10() {
    doTest(myFixture.configureByFiles("ReferenceExpression10.hx",
                                      "com/bar/Foo.hx"), 1);
  }

  public void testRegularExpression() {
    doTest(myFixture.configureByFiles("RegularExpression.hx"), 1);
  }

  public void testStringLiteral() {
    assertNotNull(myFixture);
    doTest(myFixture.configureByFiles("StringLiteral.hx"), 1);
  }

  public void testArrayLiteral() {
    doTest(myFixture.configureByFiles("ArrayLiteral.hx"), 1);
  }

  public void testAssign1() {
    doTest(myFixture.configureByFiles("Assign1.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  public void testAssign2() {
    doTest(myFixture.configureByFiles("Assign2.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  public void testNewExpression1() {
    doTest(myFixture.configureByFiles("NewExpression1.hx", "com/bar/Foo.hx"), 1);
  }

  public void testNewExpression2() {
    doTest(myFixture.configureByFiles("NewExpression2.hx", "com/bar/Foo.hx"), 1);
  }

  public void testCallFunction() {
    doTest(myFixture.configureByFiles("CallFunction.hx"), 1);
  }

  public void testGeneric1() {
    doTest(myFixture.configureByFiles("Generic1.hx"), 1);
  }

  public void testGeneric2() {
    doTest(myFixture.configureByFiles("Generic2.hx"), 1);
  }

  public void testGeneric3() {
    doTest(myFixture.configureByFiles("Generic3.hx"), 1);
  }

  public void testGeneric4() {
    doTest(myFixture.configureByFiles("Generic4.hx"), 1);
  }

  public void testGeneric5() {
    doTest(myFixture.configureByFiles("Generic5.hx"), 1);
  }

  public void testGeneric6() {
    doTest(myFixture.configureByFiles("Generic6.hx"), 1);
  }

  public void testGeneric7() {
    doTest(myFixture.configureByFiles("Generic7.hx"), 1);
  }

  public void testGeneric8() {
    doTest(myFixture.configureByFiles("Generic8.hx"), 1);
  }

  public void testTypeDef1() {
    doTest(myFixture.configureByFiles("TypeDef1.hx"), 1);
  }

  public void testTypeDef2() {
    doTest(myFixture.configureByFiles("TypeDef2.hx"), 1);
  }

  public void testTypeDef3() {
    doTest(myFixture.configureByFiles("TypeDef3.hx"), 1);
  }

  public void testTypeDef4() {
    doTest(myFixture.configureByFiles("TypeDef4.hx"), 1);
  }

  public void testTypeDef5() {
    doTest(myFixture.configureByFiles("TypeDef5.hx", "com/bar/Foo.hx"), 1);
  }

  public void testTypeDef6() { doTest(myFixture.configureByFiles("TypeDef6.hx"), 1); }

  public void testArrayAccess1() {
    doTest(myFixture.configureByFiles("ArrayAccess1.hx"), 1);
  }

  public void testArrayAccess2() {
    doTest(myFixture.configureByFiles("ArrayAccess2.hx"), 1);
  }

  public void testArrayIteration1() {
    doTest(myFixture.configureByFiles("ArrayIteration1.hx"), 1);
  }

  public void testArrayIteration2() {
    doTest(myFixture.configureByFiles("ArrayIteration2.hx"), 1);
  }

  public void testArrayIteration3() {
    doTest(myFixture.configureByFiles("ArrayIteration3.hx"), 1);
  }

  public void testHelperClass1() {
    doTest(myFixture.configureByFiles("HelperClass1.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testHelperClass2() {
    doTest(myFixture.configureByFiles("HelperClass2.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testHelperClass3() {
    doTest(myFixture.configureByFiles("HelperClass3.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testHelperClass4() {
    doTest(myFixture.configureByFiles("HelperClass4.hx", "com/utils/MathUtil.hx"), 1);
  }

  public void testCastExpression1() {
    doTest(myFixture.configureByFiles("CastExpression1.hx"), 1);
  }

  public void testTypeCheckExpression1() {
    doTest(myFixture.configureByFiles("TypeCheckExpression1.hx"), 1);
  }
}
