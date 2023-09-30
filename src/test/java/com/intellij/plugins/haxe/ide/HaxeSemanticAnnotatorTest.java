/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections;
import com.intellij.plugins.haxe.ide.annotator.HaxeUnresolvedTypeAnnotator;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.*;


public class HaxeSemanticAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                      @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                      String... additionalFiles)
    throws Exception {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    myFixture.enableInspections(getAnnotatorBasedInspection());
    registerInspectionsForTesting( new HaxeSemanticAnnotatorInspections.Registrar(), myFixture.getProject(), unsetInspections);
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
  }

  public void registerInspectionsForTesting(InspectionToolProvider provider, Project project,
                                            @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections) {
    InspectionProfileManager mgr = InspectionProfileManager.getInstance(project);
    InspectionProfileImpl profile = mgr.getCurrentProfile();

    try {
      Class<? extends LocalInspectionTool>[] classes = provider.getInspectionClasses();
      for (Class<? extends LocalInspectionTool> c : classes) {
        if (null != unsetInspections && unsetInspections.contains(c)) continue;

        Constructor<? extends LocalInspectionTool> constructor = c.getDeclaredConstructor();
        constructor.setAccessible(true);
        InspectionToolWrapper<?, ?> wrapper = new LocalInspectionToolWrapper(constructor.newInstance());

        Map<String, List<String>> dependencies = new HashMap<>();
        profile.addTool(project, wrapper, dependencies);
        profile.enableTool(wrapper.getShortName(), project);
      }
    }
    catch (Exception ex) {
      assertNotNull(ex.toString());
    }
  }

  private void doTestSkippingAnnotators(Set<Class<? extends LocalInspectionTool>> unsetInspections) throws Exception {
    doTest(true, false, false, unsetInspections);
  }

  private void doTestNoFixWithWarnings(String... additionalFiles) throws Exception {
    doTest(true, false, false, null, additionalFiles);
  }

  private void doTestNoFixWithoutWarnings(String... additionalFiles) throws Exception {
    doTest(false, false, false, null, additionalFiles);
  }

  private void doTestActions(String... filters) throws Exception {
    doTest(false, false, false, null);

    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    for (final IntentionAction action : intentions) {
      if (Arrays.asList(filters).contains(action.getText())) {
        System.out.println("Applying intent " + action.getText());
        myFixture.launchAction(action);
      }
      else {
        System.out.println("Ignoring intent " + action.getText() + ", not matching " + StringUtils.join(filters, ","));
      }
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(false) + "_expected.hx");
  }

  @Test
  public void testFixPackage() throws Exception {
    doTestActions("Fix package");
  }

  @Test
  public void testRemoveOverride() throws Exception {
    doTestActions("Remove override");
  }

  @Test
  public void testRemoveFinal() throws Exception {
    doTestActions("Remove final from Base.test");  // @:final, but the @: is no longer in the fix message.
  }

  @Test
  public void testChangeArgumentType() throws Exception {
    doTestActions(HaxeBundle.message("haxe.quickfix.change.variable.type"));
  }

  @Test
  public void testRemoveArgumentInit() throws Exception {
    doTestActions(HaxeBundle.message("haxe.quickfix.remove.initializer"));
  }

  @Test
  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOptionalFieldSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOptionalMethodSyntax() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOptionalWithInitWarning() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNonOptionalArgumentsAfterOptionalOnes() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNonConstantArgument() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNonConstantArgumentAbstractEnum() throws Exception {
    doTestNoFixWithWarnings("test/SampleAbstractEnum.hx");
  }

  @Test
  public void testConstructorMustNotBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInitMagicMethodShouldBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testRepeatedArgumentName() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractFromTo() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractFromToMetadata() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo1() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo2() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo3() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo4() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo5() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromTo6() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAbstractAssignmentFromToFunctions() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOverrideVisibility() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testUcFirstClassName() throws Exception {
    doTestActions("Change name");
  }

  @Test
  public void testUcFirstClassName2() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testRepeatedFields() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testPropertiesSimpleCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testPropertyAllowNonConstantInitialization() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOverrideSignature() throws Exception {
    doTestActions("Remove argument");
  }

  @Test
  public void testOverrideSignature2() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testOverrideSignature3() throws Exception {
    doTestActions("Remove argument");
  }

  @Test
  public void testOverrideSignature4() throws Exception {
    doTestActions("Remove argument");
  }

  @Test
  public void testOverrideSignature5() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testImplementSignature() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testImplementMethods() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testImplementExternInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testSimpleAssignUnknownGeneric() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testExtendsAnonymousType() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testExtendsSelf() throws Exception {
    doTestNoFixWithWarnings("test/Bar.hx", "test/IBar.hx", "test/TBar.hx");
  }

  @Test
  public void testFieldInitializerCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testVariableRedefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testFinalKeyword() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testFinalKeywordInInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testStaticsInExtended() throws Exception {
    doTestNoFixWithoutWarnings();
  }

  @Test
  public void testArrayAssignmentFromEmpty() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testArrayAssignmentBadFunctionType() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testArrayAssignmentWrongType() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testArrayAssignmentBadArrowFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testArrayAssignmentWithAbstract() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testArrayAssignmentWithArrowFunctions() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNullTAssignment1() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNullTAssignment2() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = 10/2;
  @Test
  public void testInitializeIntWithFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = "3.1416";
  @Test
  public void testInitializeFloatWithString() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignStringToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignStringToDynamic() throws Exception {
    doTestNoFixWithWarnings();
  }

  // var a:Int = (10.0 : Float);
  @Test
  public void testInitializeIntWithTypeCheckFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignTypeCheckFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Int = (10 : Float);
  @Test
  public void testInitializeIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignIntWithIntTypeCheckedToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:String = 3.1416;
  @Test
  public void testInitializeStringWithFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignFloatToString() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:String = 10;
  @Test
  public void testInitializeStringWithInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignIntToString() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var a:Float = 100;
  @Test
  public void testInitializeFloatWithInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignIntToFloat() throws Exception {
    doTestNoFixWithWarnings();
  }


  // var f:Float = 100; i:Int = (f);
  @Test
  public void testInitializeIntWithParenthesizedFloat() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignParenthesizedFloatToInt() throws Exception {
    doTestNoFixWithWarnings();
  }


// NOT Working yet.
  // var c:Int = {x:1, y:2};
  //@Test public void testInitializeIntWithAnonymousStruct() throws Exception {
  //  doTestNoFixWithWarnings();
  //}
  //
  //@Test public void testAssignAnonymousStructToInt() throws Exception {
  //  doTestNoFixWithWarnings();
  //}


  // typedef Pt = {x:Int; y:Int;}; var c:Int = new Pt();
  @Test
  public void testInitializeIntWithTypedef() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignTypedefToInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  // class Point {...}; var c:Int = new Point(1,2);
  @Test
  public void testInitializeIntWithClass() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignClassToInt() throws Exception {
    doTestNoFixWithWarnings();
  }

// NOT working yet.
  // class Test{ var somevar; function new() { somevar = 3.1; }
  //@Test public void testUnknownClassVariable() throws Exception {
  //  doTestNoFixWithWarnings();
  //}

  // class Test{ var somevar:Int; function new() { somevar = 3; }
  @Test
  public void testAssignFloatToTypedClassVarDeclaration() throws Exception {
    doTestNoFixWithWarnings();
  }

  // class Test{ var somevar = 10; function new() {somevar = 3.1;} }
  @Test
  public void testAssignFloatToInferredClassVarInt() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testMultipleClassModifiers() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnMultipleNullT() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoIncompatibleTypeErrorOnMap() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoIncompatibleTypeErrorOnChainedMaps() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testEitherTypeTest() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorWhenTypeParameterIsSelfClass() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorAccessingParameterizedArray() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorAccessingFieldsThroughParamaterizedMethods() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorAssigningFromParameterizedFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorAssigningToParameterizedArrayElement() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorAssigningParameterizedTypeDuringVarInit() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOverrideDefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnConstrainedGenericOverrides() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testMissingInterfaceMethodsOnConstrainedGenericOverrides() throws Exception {
      doTestNoFixWithWarnings();
  }

  //@Test public void testAssignmentOfParameterizedType() throws Exception {
  //  doTestNoFixWithWarnings();
  //}

  @Test
  public void testNoErrorOnOptionalParameterWithIntFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOptionalParameterWithSimpleStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOptionalParameterWithParenthesizedStringFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testTypeErrorOnOptionalParameterWithParenthesizedNumericFieldConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testErrorOnOptionalParameterWithNonConstMethod() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOptionalParameterWithDoublyReferencedVar() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnEnumConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnOptionalNullFloatConstant() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnConstantFunctionType() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnInlineFunctionAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnFunctionAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testNoErrorOnFunctionUnification() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInferredFunctionTypeAssignment() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testParameterizedFunctions() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testImmediateStringArrayIndexing() throws Exception {
    doTestNoFixWithWarnings();
  }

  //Issue #981
  @Test
  public void testAssignReflectionTypeToDynamic() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInitializeStringMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInitializeIntMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInitializeEnumMapWithMapLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testCrashWhileAnnotating() throws Exception {
    // A stack overflow was occurring while annotating, and there's no better place to
    // put this test at the moment, soo....
    doTestNoFixWithoutWarnings();
  }

  @Test
  public void testAssignTypeToClass() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignTypeToEnum() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignEnumValue() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testAssignEmptyCollection() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testImplicitCast() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testInitializeObjectWithGenericFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testIsKeywordFor4_2() throws Throwable {
    HashSet skipAnnotators = new HashSet();
    skipAnnotators.add(HaxeSemanticAnnotatorInspections.IsTypeExpressionInspection4dot1Compatible.class);
    doTestSkippingAnnotators(skipAnnotators);
  }

  @Test
  public void testIsKeywordFor4_1() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  @Test
  public void testEnumHasEnumValueMembers() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  @Test
  public void testCallExpression() throws Throwable {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testTypeParameterConstraints() throws Throwable {
    doTestSkippingAnnotators(new HashSet<>());
  }

  @Test
  public void testStringInterpolation() throws Throwable {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testLiteralCollectionArguments() throws Throwable {
    doTestNoFixWithWarnings();
  }

  @Test
  public void testTypeParameterCount() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  public void testAssignFromRecursiveMethod() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  public void testMethodRestArguments() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  public void testOperatorPrimitiveTest() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  public void testOperatorAbstractTest() throws Throwable {
    doTestNoFixWithWarnings();
  }
  @Test
  public void testResolveTypeFromUsage() throws Throwable {
    doTestNoFixWithWarnings();
  }
}
