/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public class HaxeSemanticAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  private void doTestInternal(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings, String... additionalFiles) throws Exception {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, new HaxeTypeAnnotator());
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
  }

  private void doTestNoFixWithWarnings(String... additionalFiles) throws Exception {
    doTestInternal(true, false, false, additionalFiles);
  }

  private void doTestNoFixWithoutWarnings(String... additionalFiles) throws Exception {
    doTestInternal(false, false, false, additionalFiles);
  }

  private void doTest(String... filters) throws Exception {
    doTestInternal(false, false, false);

    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    for (final IntentionAction action : intentions) {
      if (Arrays.asList(filters).contains(action.getText())) {
        System.out.println("Applying intent " + action.getText());
        myFixture.launchAction(action);
      } else {
        System.out.println("Ignoring intent " + action.getText() + ", not matching " + StringUtils.join(filters, ","));
      }
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(false) + "_expected.hx");
  }

  public void testFixPackage() throws Exception {
    doTest("Fix package");
  }

  public void testRemoveOverride() throws Exception {
    doTest("Remove override");
  }

  public void testRemoveFinal() throws Exception {
    doTest("Remove @:final from Base.test");
  }

  public void testChangeArgumentType() throws Exception {
    doTest(HaxeBundle.message("haxe.quickfix.change.variable.type"));
  }

  public void testRemoveArgumentInit() throws Exception {
    doTest(HaxeBundle.message("haxe.quickfix.remove.initializer"));
  }

  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOptionalWithInitWarning() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonOptionalArgumentsAfterOptionalOnes() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgument() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgumentAbstractEnum() throws Exception {
    doTestNoFixWithWarnings("test/SampleAbstractEnum.hx", "std/StdTypes.hx");
  }

  public void testConstructorMustNotBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitMagicMethodShouldBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedArgumentName() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractFromTo() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractAssignmentFromTo1() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testAbstractAssignmentFromTo2() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testAbstractAssignmentFromTo3() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testAbstractAssignmentFromTo4() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testAbstractAssignmentFromTo5() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testAbstractAssignmentFromTo6() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideVisibility() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testUcFirstClassName() throws Exception {
    doTest("Change name");
  }

  public void testUcFirstClassName2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedFields() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertiesSimpleCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertyAllowNonConstantInitialization() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature() throws Exception {
    doTest("Remove argument");
  }

  public void testOverrideSignature2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature3() throws Exception {
    doTest("Remove argument");
  }

  public void testOverrideSignature4() throws Exception {
    doTest("Remove argument");
  }

  public void testOverrideSignature5() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testImplementSignature() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplementExternInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testSimpleAssignUnknownGeneric() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testExtendsAnonymousType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testExtendsSelf() throws Exception {
    doTestNoFixWithWarnings("test/Bar.hx", "test/IBar.hx", "test/TBar.hx");
  }

  public void testFieldInitializerCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testVariableRedefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testFinalKeyword() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testStaticsInExtended() throws Exception {
    doTestNoFixWithoutWarnings();
  }

  public void testArrayAssignmentFromEmpty() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentBadFunctionType() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWrongType() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentBadArrowFunction() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWithAbstract() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWithArrowFunctions() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testNullTAssignment1() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullTAssignment2() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = 10/2;
  public void testInitializeIntWithFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignFloatToInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:Int = "3.1416";
  public void testInitializeFloatWithString() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignStringToFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:Int = (10.0 : Float);
  public void testInitializeIntWithTypeCheckFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignTypeCheckFloatToInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:Int = (10 : Float);
  public void testInitializeIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:String = 3.1416;
  public void testInitializeStringWithFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignFloatToString() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:String = 10;
  public void testInitializeStringWithInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignIntToString() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var a:Float = 100;
  public void testInitializeFloatWithInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignIntToFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


  // var f:Float = 100; i:Int = (f);
  public void testInitializeIntWithParenthesizedFloat() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignParenthesizedFloatToInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }


// NOT Working yet.
  // var c:Int = {x:1, y:2};
  //public void testInitializeIntWithAnonymousStruct() throws Exception {
  //  doTestNoFixWithWarnings("std/StdTypes.hx");
  //}
  //
  //public void testAssignAnonymousStructToInt() throws Exception {
  //  doTestNoFixWithWarnings("std/StdTypes.hx");
  //}


  // typedef Pt = {x:Int; y:Int;}; var c:Int = new Pt();
  public void testInitializeIntWithTypedef() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignTypedefToInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  // class Point {...}; var c:Int = new Point(1,2);
  public void testInitializeIntWithClass() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testAssignClassToInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

// NOT working yet.
  // class Test{ var somevar; function new() { somevar = 3.1; }
  //public void testUnknownClassVariable() throws Exception {
  //  doTestNoFixWithWarnings("std/StdTypes.hx");
  //}

  // class Test{ var somevar:Int; function new() { somevar = 3; }
  public void testAssignFloatToTypedClassVarDeclaration() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  // class Test{ var somevar = 10; function new() {somevar = 3.1;} }
  public void testAssignFloatToInferredClassVarInt() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testNoErrorOnMultipleNullT() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/String.hx");
  }

  public void testNoIncompatibleTypeErrorOnMap() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Map.hx", "std/String.hx");
  }

  public void testNoIncompatibleTypeErrorOnChainedMaps() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Map.hx", "std/String.hx");
  }

  public void testEitherTypeTest() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/String.hx", "std/EitherType.hx");
  }

  public void testNoErrorWhenTypeParameterIsSelfClass() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testNoErrorAccessingParameterizedArray() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testNoErrorAccessingFieldsThroughParamaterizedMethods() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testNoErrorAssigningToParameterizedArrayElement() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testNoErrorAssigningParameterizedTypeDuringVarInit() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testNoErrorOnOverrideDefinition() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx");
  }

  public void testNoErrorOnConstrainedGenericOverrides() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Map.hx", "std/Class.hx");
  }

  //public void testAssignmentOfParameterizedType() throws Exception {
  //  doTestNoFixWithWarnings();
  //}

  public void testNoErrorOnOptionalParameterWithIntFieldConstant() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx");
  }

  public void testNoErrorOnOptionalParameterWithSimpleStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx", "std/String.hx");
  }

  public void testNoErrorOnOptionalParameterWithParenthesizedStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx", "std/String.hx");
  }

  public void testNoErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx", "std/String.hx");
  }

  public void testErrorOnOptionalParameterWithNonConstMethod() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx", "std/String.hx");
  }

  public void testNoErrorOnOptionalParameterWithDoublyReferencedVar() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Std.hx");
  }

  public void testNoErrorOnEnumConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

}
