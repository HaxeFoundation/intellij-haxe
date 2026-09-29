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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeObjectLiteral;
import com.intellij.plugins.haxe.lang.psi.HaxeTypedefDeclaration;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import java.util.Collection;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Navigation: go to declaration action")
public class HaxeGoToDeclarationActionTest extends HaxeToolkitLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/goto/";
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

  @Test
  @DisplayName("var declaration")
  public void testVarDeclaration() {
    doTest(myFixture.configureByFiles("VarDeclaration.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("function parameter")
  public void testFunctionParameter() {
    doTest(myFixture.configureByFiles("FunctionParameter.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("interface parameter")
  public void testInterfaceParameter() {
    doTest(myFixture.configureByFiles("InterfaceDeclaration.hx", "com/bar/IBar.hx"), 1);
  }

  @Test
  @DisplayName("for declaration 1 - loop var over int range")
  public void testForDeclaration1() {
    doTest(myFixture.configureByFiles("ForDeclaration1.hx"), 1);
  }

  @Test
  @DisplayName("for declaration 2 - loop var used as array index")
  public void testForDeclaration2() {
    doTest(myFixture.configureByFiles("ForDeclaration2.hx"), 1);
  }

  @Test
  @DisplayName("local var declaration 1 - reference in same scope")
  public void testLocalVarDeclaration1() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration1.hx"), 1);
  }

  @Test
  @DisplayName("local var declaration 2 - reference in nested if block")
  public void testLocalVarDeclaration2() {
    doTest(myFixture.configureByFiles("LocalVarDeclaration2.hx"), 1);
  }

  @Test
  @DisplayName("function parameter 1 - reference in nested block")
  public void testFunctionParameter1() {
    doTest(myFixture.configureByFiles("FunctionParameter1.hx"), 1);
  }

  @Test
  @DisplayName("function parameter 2 - lambda parameter reference")
  public void testFunctionParameter2() {
    doTest(myFixture.configureByFiles("FunctionParameter2.hx"), 1);
  }

  @Test
  @DisplayName("reference")
  public void testReference() {
    doTest(myFixture.configureByFiles("Reference.hx"), 1);
  }

  @Test
  @DisplayName("this expression")
  public void testThisExpression() {
    doTest(myFixture.configureByFiles("ThisExpression.hx"), 1);
  }

  @Test
  @DisplayName("this shadowing")
  public void testThisShadowing() {
    doTest(myFixture.configureByFiles("ThisShadowing.hx"), 0);
  }

  @Test
  @DisplayName("static class member 1 - static field through class")
  public void testStaticClassMember1() {
    doTest(myFixture.configureByFiles("StaticClassMember1.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("static class member 2 - class reference before static field")
  public void testStaticClassMember2() {
    doTest(myFixture.configureByFiles("StaticClassMember2.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("function call")
  public void testFunctionCall() {
    doTest(myFixture.configureByFiles("FunctionCall.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("using util 1 - static extension on int receiver")
  public void testUsingUtil1() {
    doTest(myFixture.configureByFiles("UsingUtil1.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("using util 2 - extension not applicable to string receiver")
  public void testUsingUtil2() {
    doTest(myFixture.configureByFiles("UsingUtil2.hx", "com/utils/MathUtil.hx"), 0);
  }

  @Test
  @DisplayName("using util 3 - string extension via tools using")
  public void testUsingUtil3() {
    doTest(myFixture.configureByFiles("UsingUtil3.hx", "com/utils/StringUtil.hx", "com/utils/Tools.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("same package")
  public void testSamePackage() {
    doTest(myFixture.configureByFiles("com/bar/Baz.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("extern class 1 - extern type in field type tag")
  public void testExternClass1() {
    doTest(myFixture.configureByFiles("ExternClass1.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("extern class 2 - private extern class in same file")
  public void testExternClass2() {
    doTest(myFixture.configureByFiles("ExternClass2.hx"), 1);
  }

  @Test
  @DisplayName("super field")
  public void testSuperField() {
    doTest(myFixture.configureByFiles("SuperField.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 1 - field through typed local")
  public void testReferenceExpression1() {
    doTest(myFixture.configureByFiles("ReferenceExpression1.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 2 - chained field access")
  public void testReferenceExpression2() {
    doTest(myFixture.configureByFiles("ReferenceExpression2.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 3 - middle link of a chain")
  public void testReferenceExpression3() {
    doTest(myFixture.configureByFiles("ReferenceExpression3.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 4 - field declared in superclass")
  public void testReferenceExpression4() {
    doTest(myFixture.configureByFiles("ReferenceExpression4.hx",
                                      "com/bar/Foo.hx",
                                      "com/bar/Baz.hx",
                                      "com/bar/IBar.hx",
                                      "com/bar/SuperClass.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 5 - field on extern class instance")
  public void testReferenceExpression5() {
    doTest(myFixture.configureByFiles("ReferenceExpression5.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("reference expression 6 - use before var declaration")
  public void testReferenceExpression6() {
    doTest(myFixture.configureByFiles("ReferenceExpression6.hx"), 0);
  }

  @Test
  @DisplayName("reference expression 7 - use before local function declaration")
  public void testReferenceExpression7() {
    doTest(myFixture.configureByFiles("ReferenceExpression7.hx"), 0);
  }

  @Test
  @DisplayName("reference expression 8 - outer var from nested function")
  public void testReferenceExpression8() {
    doTest(myFixture.configureByFiles("ReferenceExpression8.hx"), 0);
  }

  @Test
  @DisplayName("reference expression 9 - switch case capture variable")
  public void testReferenceExpression9() {
    doTest(myFixture.configureByFiles("ReferenceExpression9.hx"), 0);
  }

  @Test
  @DisplayName("reference expression 10 - superclass reference in extends")
  public void testReferenceExpression10() {
    doTest(myFixture.configureByFiles("ReferenceExpression10.hx",
                                      "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("regular expression")
  public void testRegularExpression() {
    doTest(myFixture.configureByFiles("RegularExpression.hx"), 1);
  }

  @Test
  @DisplayName("string literal")
  public void testStringLiteral() {
    assertNotNull(myFixture);
    doTest(myFixture.configureByFiles("StringLiteral.hx"), 1);
  }

  @Test
  @DisplayName("array literal")
  public void testArrayLiteral() {
    doTest(myFixture.configureByFiles("ArrayLiteral.hx"), 1);
  }

  @Test
  @DisplayName("assign 1 - type flows through local assignment")
  public void testAssign1() {
    doTest(myFixture.configureByFiles("Assign1.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  @Test
  @DisplayName("assign 2 - type flows from field access assignment")
  public void testAssign2() {
    doTest(myFixture.configureByFiles("Assign2.hx", "com/bar/Foo.hx", "com/bar/Baz.hx"), 1);
  }

  @Test
  @DisplayName("new expression 1 - member through constructed local")
  public void testNewExpression1() {
    doTest(myFixture.configureByFiles("NewExpression1.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("new expression 2 - member directly on new expression")
  public void testNewExpression2() {
    doTest(myFixture.configureByFiles("NewExpression2.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("call function")
  public void testCallFunction() {
    doTest(myFixture.configureByFiles("CallFunction.hx"), 1);
  }

  @Test
  @DisplayName("generic 1 - member via resolved type parameter")
  public void testGeneric1() {
    doTest(myFixture.configureByFiles("Generic1.hx"), 1);
  }

  @Test
  @DisplayName("generic 2 - nested generic resolves through two levels")
  public void testGeneric2() {
    doTest(myFixture.configureByFiles("Generic2.hx"), 1);
  }

  @Test
  @DisplayName("generic 3 - type parameter reference in signature")
  public void testGeneric3() {
    doTest(myFixture.configureByFiles("Generic3.hx"), 1);
  }

  @Test
  @DisplayName("generic 4 - array pop element member")
  public void testGeneric4() {
    doTest(myFixture.configureByFiles("Generic4.hx"), 1);
  }

  @Test
  @DisplayName("generic 5 - inherited generic array member")
  public void testGeneric5() {
    doTest(myFixture.configureByFiles("Generic5.hx"), 1);
  }

  @Test
  @DisplayName("generic 6 - self referencing generic array")
  public void testGeneric6() {
    doTest(myFixture.configureByFiles("Generic6.hx"), 1);
  }

  @Test
  @DisplayName("generic 7 - method through generic superclass chain")
  public void testGeneric7() {
    doTest(myFixture.configureByFiles("Generic7.hx"), 1);
  }

  @Test
  @DisplayName("generic 8 - member via constrained type parameter")
  public void testGeneric8() {
    doTest(myFixture.configureByFiles("Generic8.hx"), 1);
  }

  @Test
  @DisplayName("type def 1 - member of extended anonymous type")
  public void testTypeDef1() {
    doTest(myFixture.configureByFiles("TypeDef1.hx"), 1);
  }

  @Test
  @DisplayName("type def 2 - generic typedef field member")
  public void testTypeDef2() {
    doTest(myFixture.configureByFiles("TypeDef2.hx"), 1);
  }

  @Test
  @DisplayName("type def 3 - generic typedef function member")
  public void testTypeDef3() {
    doTest(myFixture.configureByFiles("TypeDef3.hx"), 1);
  }

  @Test
  @DisplayName("type def 4 - inherited field through typedef alias")
  public void testTypeDef4() {
    doTest(myFixture.configureByFiles("TypeDef4.hx"), 1);
  }

  @Test
  @DisplayName("type def 5 - local typed by typedef alias")
  public void testTypeDef5() {
    doTest(myFixture.configureByFiles("TypeDef5.hx", "com/bar/Foo.hx"), 1);
  }

  @Test
  @DisplayName("type def 6 - typedef extension references")
  public void testTypeDef6() {
    doTest(myFixture.configureByFiles("TypeDef6.hx"), 1);
  }

  @Test
  @DisplayName("array access 1 - element member after index")
  public void testArrayAccess1() {
    doTest(myFixture.configureByFiles("ArrayAccess1.hx"), 1);
  }

  @Test
  @DisplayName("array access 2 - element member after nested index")
  public void testArrayAccess2() {
    doTest(myFixture.configureByFiles("ArrayAccess2.hx"), 1);
  }

  @Test
  @DisplayName("array iteration 1 - loop var member from array")
  public void testArrayIteration1() {
    doTest(myFixture.configureByFiles("ArrayIteration1.hx"), 1);
  }

  @Test
  @DisplayName("array iteration 2 - loop var member from custom iterator")
  public void testArrayIteration2() {
    doTest(myFixture.configureByFiles("ArrayIteration2.hx"), 1);
  }

  @Test
  @DisplayName("array iteration 3 - loop var shadows iterator name")
  public void testArrayIteration3() {
    doTest(myFixture.configureByFiles("ArrayIteration3.hx"), 1);
  }

  @Test
  @DisplayName("helper class 1 - module member via module import")
  public void testHelperClass1() {
    doTest(myFixture.configureByFiles("HelperClass1.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("helper class 2 - module member imported directly")
  public void testHelperClass2() {
    doTest(myFixture.configureByFiles("HelperClass2.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("helper class 3 - target inside import statement")
  public void testHelperClass3() {
    doTest(myFixture.configureByFiles("HelperClass3.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("helper class 4 - module member via qualified type tag")
  public void testHelperClass4() {
    doTest(myFixture.configureByFiles("HelperClass4.hx", "com/utils/MathUtil.hx"), 1);
  }

  @Test
  @DisplayName("cast expression 1 - member on cast result")
  public void testCastExpression1() {
    doTest(myFixture.configureByFiles("CastExpression1.hx"), 1);
  }

  @Test
  @DisplayName("type check expression 1 - member on type checked expression")
  public void testTypeCheckExpression1() {
    doTest(myFixture.configureByFiles("TypeCheckExpression1.hx"), 1);
  }

  /**
   * (fixture, resolved field, its declaring typedef): the object-literal-key
   * shapes go-to-declaration must cross — a plain typedef, an extends parent,
   * an intersection, and literals nested in constructor arguments (plain,
   * array element, intersection, optional array).
   */
  static final List<Arguments> OBJECT_LITERAL_KEYS = List.of(
    arguments("ObjectLiteralKey.hx", "name", "Config"),
    arguments("ObjectLiteralKeyExtends.hx", "label", "Base"),
    arguments("ObjectLiteralKeyIntersection.hx", "alpha", "Combo"),
    arguments("ConstructorArgKey.hx", "items", "ContainerConfig"),
    arguments("ConstructorArgNestedArrayKey.hx", "itemId", "ItemConfig"),
    arguments("ConstructorArgIntersectionKey.hx", "flag", "ExtraConfig"),
    arguments("ConstructorArgOptionalArrayKey.hx", "key", "EntryConfig"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("OBJECT_LITERAL_KEYS")
  @DisplayName("object literal key resolves to its typedef field")
  public void testObjectLiteralKeyResolvesToItsTypedefField(String fixture, String field, String typedefName) {
    myFixture.configureByFiles(fixture);
    PsiElement target = singleGotoTarget();
    assertEquals(field, resolvedName(target));
    assertNull(PsiTreeUtil.getParentOfType(target, HaxeObjectLiteral.class, false),
               "should navigate to the typedef field, not stay on the object literal key");
    HaxeTypedefDeclaration typedef = PsiTreeUtil.getParentOfType(target, HaxeTypedefDeclaration.class);
    assertNotNull(typedef, "target should be declared inside a typedef");
    assertEquals(typedefName, typedef.getComponentName().getText());
  }

  /** Runs the real go-to-declaration pipeline (handlers + reference resolution) and expects a single target. */
  private PsiElement singleGotoTarget() {
    PsiElement[] targets = GotoDeclarationAction.findAllTargetElements(
      myFixture.getProject(), myFixture.getEditor(), myFixture.getCaretOffset());
    assertNotNull(targets);
    assertEquals(1, targets.length, "expected exactly one navigation target");
    return targets[0];
  }

  private static String resolvedName(PsiElement target) {
    if (target instanceof HaxeComponentName componentName) {
      return componentName.getText();
    }
    HaxeComponentName componentName = PsiTreeUtil.findChildOfType(target, HaxeComponentName.class);
    return componentName != null ? componentName.getText() : target.getText();
  }
}
